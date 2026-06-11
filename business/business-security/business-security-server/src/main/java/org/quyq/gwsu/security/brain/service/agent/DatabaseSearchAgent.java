package org.quyq.gwsu.security.brain.service.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.session.Session;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.subagent.SubAgentConfig;
import io.agentscope.harness.agent.hook.AgentTraceHook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.SessionUtils;
import org.quyq.gwsu.security.api.tablemodel.vo.BusinessFunctionDetailVO;
import org.quyq.gwsu.security.api.tablemodel.vo.BusinessFunctionVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelTableVO;
import org.quyq.gwsu.security.brain.ModelProvider;
import org.quyq.gwsu.security.brain.service.tool.DatabaseSearchTool;
import org.quyq.gwsu.security.role.service.ISecurityRoleTableModelService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityBusinessFunctionService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelTableService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSearchAgent {

    private final ObjectProvider<Memory> memoryProvider;

    private final ObjectProvider<Toolkit> toolkitProvider;

    private final Session agentSession;

    private final SecurityUtils securityUtils;

    private final SessionUtils sessionUtils;

    private final ISecurityTableModelTableService tableModelTableService;

    private final ISecurityRoleTableModelService roleTableModelService;

    private final ISecurityBusinessFunctionService businessFunctionService;

    private final DatabaseSearchTool databaseSearchTool;

    private final static String TABLE_MODEL_PERMISSION_KEY = "tableModelPermission";

    public Map<String, Map<String, FieldPermission>> getUserTableModelPermission() {
        Optional<Map<String, Map<String, FieldPermission>>> value = sessionUtils.getValue(TABLE_MODEL_PERMISSION_KEY);
        if (value.isPresent()) {
            return value.get();
        }
        List<String> roles = securityUtils.getSubject()
                .map(Subject::getRoles)
                .orElse(Collections.emptyList());

        Map<String, Map<String, FieldPermission>> mergedPermissions = Map.of();
        if (!CollectionUtils.isEmpty(roles)) {
            mergedPermissions = roleTableModelService.getMergedRoleTableModelPermission(roles);
        }

        sessionUtils.putValue(TABLE_MODEL_PERMISSION_KEY, mergedPermissions);

        return mergedPermissions;
    }

    public ReActAgent build() {
        Memory memory = memoryProvider.getIfAvailable();
        Toolkit toolkit = toolkitProvider.getIfAvailable(Toolkit::new);

        AgentSkill skill = buildDatabaseSearchSkill();
        SkillBox skillBox = new SkillBox(toolkit);
        skillBox.registration()
                .skill(skill)
                .tool(databaseSearchTool)
                .apply();

        return ReActAgent.builder()
                .name("SelectiveSQLAgent")
                .sysPrompt(buildSystemPrompt())
                .memory(memory)
                .model(ModelProvider.generateModel())
                .toolkit(toolkit)
                .skillBox(skillBox)
                .toolExecutionContext(ToolExecutionContext.builder()
                        .register(DatabaseSearchAgent.class, this)
                        .build())
                .hook(new AgentTraceHook())
                .build();
    }

    private AgentSkill buildDatabaseSearchSkill() {
        AgentSkill.Builder skillBuilder = AgentSkill.builder()
                .name("database_search")
                .description("""
                        根据自然语言问题生成SQL并执行查询。优先识别用户是否命中已有业务功能（如销售额分析），若命中则优先查阅对应业务文档获取业务规则、表模型和权限信息，结合表模型概览和元数据工具（GetDatabaseVendor、GetTableDetail）确认字段、权限和外键后生成符合数据库厂商语法的SELECT语句，并可调用ExecuteSql执行查询。严格遵守同一数据源内关联、禁止SELECT *、默认添加10条限制、仅SELECT操作等约束。
                        """);

        List<TableModelTableVO> allTables = tableModelTableService.listAll();
        if(CollectionUtils.isEmpty(allTables)) {
            return skillBuilder
                    .skillContent("系统表模型未初始化，该功能不可用，请回复用户，让其`联系管理员在'AI表模型管理'中采集所有表模型`")
                    .build();


        }

        Map<String, Map<String, FieldPermission>> mergedPermissions = getUserTableModelPermission();

        Map<String, Boolean> tablePermissionMap = buildTablePermissionMap(allTables, mergedPermissions);

        boolean isSingle = DeployUtils.isSingle();
        String groupCondition = isSingle ? "同一数据源" : "同一服务同一数据源";

        Map<String, List<TableModelTableVO>> groupedTables;
        if (isSingle) {
            groupedTables = allTables.stream()
                    .collect(Collectors.groupingBy(
                            TableModelTableVO::getDataSource,
                            LinkedHashMap::new,
                            Collectors.toList()));
        } else {
            groupedTables = allTables.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getModulePrefix() + ":" + t.getDataSource(),
                            LinkedHashMap::new,
                            Collectors.toList()));
        }

        Map<String, List<TableModelTableVO>> finalGroupedTables = groupedTables;

        String skillContent = buildSkillContent(groupCondition, isSingle);

        String tableOverviewContent = buildTableOverviewResource(finalGroupedTables, tablePermissionMap, isSingle);

        Map<String, String> businessResources = buildBusinessFunctionResources(allTables, tablePermissionMap);


        skillBuilder.skillContent(skillContent)
                .addResource("reference/table_overview.md", tableOverviewContent);

        businessResources.forEach(skillBuilder::addResource);

        return skillBuilder.build();
    }

    private Map<String, Boolean> buildTablePermissionMap(
            List<TableModelTableVO> allTables,
            Map<String, Map<String, FieldPermission>> mergedPermissions) {
        Map<String, Boolean> tablePermissionMap = new LinkedHashMap<>();
        for (TableModelTableVO table : allTables) {
            String key = table.getModulePrefix() + ":" + table.getDataSource() + ":" + table.getTableName();
            Map<String, FieldPermission> fieldPerms = mergedPermissions.get(key);
            tablePermissionMap.put(key, Objects.nonNull(fieldPerms));
        }
        return tablePermissionMap;
    }

    private String buildSkillContent(String groupCondition, boolean isSingle) {
        String deployMode = isSingle ? "单应用部署" : "微服务部署";

        StringBuilder busContent = new StringBuilder();

        busContent.append("| 业务名称 | 简介 | 详细文档 |\n");
        busContent.append("|---------|------|---------|\n");

        List<BusinessFunctionVO> businessFunctions = businessFunctionService.listAll();
        if (!CollectionUtils.isEmpty(businessFunctions)) {
            for (BusinessFunctionVO bf : businessFunctions) {
                String sanitizedName = sanitizeFileName(bf.getName());
                busContent.append("| ").append(bf.getName())
                        .append(" | ").append(bf.getSummary() != null ? bf.getSummary() : "-")
                        .append(" | [查看详情](reference/").append(sanitizedName).append(".md) |\n");
            }
        } else {
            busContent.append("| - | 暂无业务功能配置 | - |\n");
        }


        return """
                # 数据查询技能
                
                ## 核心工作流程（必须严格遵守）
                
                **第一步：识别业务场景并获取理解**
                - 仔细分析用户问题，判断是否可能属于【可用业务功能】中列出的任一场景（如“销售额分析”）。
                - **如果可能属于**：建议优先阅读该业务对应的【详细文档】。该文档可能包含相关表模型、权限、业务规则、状态定义和典型 SQL 示例，阅读文档有助于加深对业务逻辑的理解。但请注意：文档内容可能不完整或与实际情况有差异，**后续仍需结合表模型概览、元数据工具和您的专业知识综合判断**。
                - **如果不属于任何已有业务功能**：则进入“通用查询模式”，参考【数据库表模型概览】和通用 SQL 规范生成查询。
                
                **第二步：获取必要信息**
                - 在生成 SQL 之前，需要获取以下两类信息（两者没有固定的优先级顺序，按需调用）：
                  - **数据库厂商类型**：调用 `GetDatabaseVendor` 工具，用于确定 SQL 语法（例如分页语法、函数等）。
                  - **表字段详情**：调用 `GetTableDetail` 工具，获取相关表的字段信息、字段权限、以及外键关联关系。
                - **注意**：
                  - 如果已经通过业务文档或表模型概览确认了完整的表结构、权限和外键，且确信信息准确，可以省略工具调用，但通常建议至少调用 `GetDatabaseVendor`。
                  - 业务文档可能过时，优先以工具返回的实际结构为准。
                
                **第三步：生成 SQL 语句**
                - 严格遵循对应业务文档中的业务规则（如果已查阅文档）。如果文档内容与实际表结构冲突，以实际表结构为准。
                - 如果使用通用查询模式，则遵循下方【重要约束】中的通用规则。
                - 禁止使用 `SELECT *`，必须明确列出字段。
                - 默认加上 10 条数据限制（除非用户明确要求更多），并根据数据库厂商使用正确语法（MySQL/PostgreSQL 用 `LIMIT 10`，Oracle 用 `FETCH FIRST 10 ROWS ONLY`，SQL Server 用 `TOP 10`），返回时告知用户限制情况。
                
                **第四步：执行 SQL**
                - 如果用户需要执行生成的 SQL 并获取结果，可以调用 `ExecuteSql` 工具。
                - **执行前提**：SQL 中涉及的所有表模型和字段，必须是在本技能中已确认存在且有权限的（例如通过业务文档、表模型概览或 `GetTableDetail` 工具查询到的）。若不满足此条件，执行将会失败。
                
                ---
                
                ## 可用业务功能
                
                %s
                
                > 如果用户需求命中上述业务，你必须优先查阅对应的详细文档。文档内容可能包含表模型、权限、业务规则和 SQL 示例，但应以实际数据库结构为准。
                
                ---
                
                ## 数据库表模型概览
                
                若用户需求**不涉及**任何已有业务功能，则参考 [表模型概览]（reference/table_overview.md）了解有哪些可用表及其所属数据源和权限。 \s
                注意：该概览仅包含表级元信息，不包含字段详情和业务规则。详细字段信息请通过 `GetTableDetail` 工具获取。
                
                ---
                
                ## 部署模式
                
                当前为**%s**：
                - 单应用部署：按**数据源**分组，同一数据源下的表可以在一条 SQL 中关联查询
                - 微服务部署：按**服务名+数据源**分组，同一服务同一数据源下的表才可关联
                
                当前分组条件：**%s**下的表可以关联查询
                
                ---
                
                ## 重要约束
                
                - 同一条 SQL 中只能关联**%s**下的表，不同分组不能跨组查询
                - 如用户需求涉及不同分组，须生成多条 SQL 分别执行
                - 仅允许生成 SELECT 语句，禁止任何修改操作
                - 禁止使用 `SELECT *`
                - 无权限的表和字段不得使用
                - 默认添加 10 条数据限制（见第三步）
                - **优先使用业务文档中的业务规则（如有），但文档与实际结构矛盾时以实际为准**
                - **执行 SQL 前必须确保所有表和字段在本技能中已确认存在且有权限**，否则 `ExecuteSql` 会失败
                
                ---
                
                ## 使用说明（补充参考）
                
                1. ✅ **首要步骤**：判断业务场景 → 命中则优先阅读业务文档加深理解 → 综合文档和表结构生成 SQL
                2. ⚠️ **次选步骤**：未命中业务 → 读表模型概览 → 按需调用元数据工具 → 生成通用 SQL
                3. 生成 SQL 前调用 `GetDatabaseVendor` 确认数据库厂商类型，调用 `GetTableDetail` 确认字段、权限和外键（两者无先后顺序）
                4. 如果业务文档内容与表模型概览或元数据工具返回的实际表结构不一致，请以实际表结构为准
                5. 如需执行 SQL，使用 `ExecuteSql` 工具，执行前确保已获取了表和字段的权限存在性（可基于已获取的信息判断）
                
                """.formatted(busContent.toString(),deployMode,groupCondition,groupCondition);
    }

    private String buildTableOverviewResource(
            Map<String, List<TableModelTableVO>> groupedTables,
            Map<String, Boolean> tablePermissionMap,
            boolean isSingle) {

        Map<String, String> tableBusinessMap = buildTableBusinessMap();

        StringBuilder tables = new StringBuilder();

        for (Map.Entry<String, List<TableModelTableVO>> entry : groupedTables.entrySet()) {
            if (isSingle) {
                String dataSource = entry.getKey();
                tables.append("## 分组：").append(dataSource).append("（数据源）\n\n");
            } else {
                String[] parts = entry.getKey().split(":", 2);
                String modulePrefix = parts[0];
                String dataSource = parts.length > 1 ? parts[1] : "master";
                tables.append("## 分组：").append(modulePrefix).append(":").append(dataSource).append("（服务:数据源）\n\n");

            }

            tables.append("| 表名 | 表注释 | 所属业务 | 有权限 |\n");
            tables.append("|------|--------|---------|--------|\n");
            for (TableModelTableVO table : entry.getValue()) {
                String key = table.getModulePrefix() + ":" + table.getDataSource() + ":" + table.getTableName();
                boolean hasPermission = tablePermissionMap.getOrDefault(key, true);
                String businesses = tableBusinessMap.getOrDefault(table.getTableName(), "-");
                tables.append("| ").append(table.getTableName())
                        .append(" | ").append(table.getTableComment() != null ? table.getTableComment() : "-")
                        .append(" | ").append(businesses)
                        .append(" | ").append(hasPermission ? "是" : "否")
                        .append(" |\n");
            }

        }

        return """
                # 数据库表模型概览
                
                > **使用说明**：本文件仅提供表级别的概览（表名、注释、所属业务、权限）。 \s
                > **优先使用原则**：如果用户问题匹配【可用业务功能】中的某个业务，请优先查阅该业务对应的详细文档，其中可能包含相关表的字段结构、业务规则和 SQL 示例。 \s
                > **本概览的使用场景**：仅当用户需求不属于任何已有业务功能，或者需要快速了解有哪些可用表时，才使用本概览。此时如需字段详情，请调用 `GetTableDetail` 工具。
                
                %s
                
                > 注意：必须属于同一个分组的表才能在一条 SQL 中关联查询。
                """.formatted(tables.toString());
    }

    private Map<String, String> buildBusinessFunctionResources(
            List<TableModelTableVO> allTables,
            Map<String, Boolean> tablePermissionMap) {

        Map<String, String> resources = new LinkedHashMap<>();

        List<BusinessFunctionVO> businessFunctions = businessFunctionService.listAll();
        if (CollectionUtils.isEmpty(businessFunctions)) {
            return resources;
        }

        for (BusinessFunctionVO bf : businessFunctions) {
            BusinessFunctionDetailVO detail = businessFunctionService.getDetailById(bf.getId());

            StringBuilder sb = new StringBuilder();
            sb.append("# ").append(bf.getName()).append("\n\n");

            sb.append("# 业务简介\n");
            sb.append(bf.getSummary() != null ? bf.getSummary() : "-").append("\n\n");

            if (bf.getDetail() != null && !bf.getDetail().isBlank()) {
                sb.append("# 详细介绍\n\n");
                sb.append(bf.getDetail()).append("\n\n");
            }

            if (detail != null && !CollectionUtils.isEmpty(detail.getTables())) {
                sb.append("# 关联表模型\n\n");
                sb.append("| 服务 | 数据源 | 表名 | 表注释 | 当前用户权限 |\n");
                sb.append("|------|--------|------|--------|-------------|\n");
                for (TableModelTableVO table : detail.getTables()) {
                    String key = table.getModulePrefix() + ":" + table.getDataSource() + ":" + table.getTableName();
                    boolean hasPermission = tablePermissionMap.getOrDefault(key, true);
                    sb.append("| ").append(table.getModulePrefix() != null ? table.getModulePrefix() : "-")
                            .append(" | ").append(table.getDataSource() != null ? table.getDataSource() : "-")
                            .append(" | ").append(table.getTableName())
                            .append(" | ").append(table.getTableComment() != null ? table.getTableComment() : "-")
                            .append(" | ").append(hasPermission ? "✅ 有权限" : "❌ 无权限")
                            .append(" |\n");
                }
                sb.append("\n");
            }

            String sanitizedName = sanitizeFileName(bf.getName());
            resources.put("reference/" + sanitizedName + ".md", sb.toString());
        }

        return resources;
    }

    private Map<String, String> buildTableBusinessMap() {
        Map<String, String> tableBusinessMap = new LinkedHashMap<>();
        List<BusinessFunctionVO> businessFunctions = businessFunctionService.listAll();
        if (CollectionUtils.isEmpty(businessFunctions)) {
            return tableBusinessMap;
        }
        for (BusinessFunctionVO bf : businessFunctions) {
            BusinessFunctionDetailVO detail = businessFunctionService.getDetailById(bf.getId());
            if (detail != null && !CollectionUtils.isEmpty(detail.getTables())) {
                for (TableModelTableVO table : detail.getTables()) {
                    tableBusinessMap.merge(
                            table.getTableName(),
                            bf.getName(),
                            (existing, newName) -> existing + ", " + newName
                    );
                }
            }
        }
        return tableBusinessMap;
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private String buildSystemPrompt() {
        return """
                # 角色定义
                你是一个专业的SQL查询生成助手，拥有以下能力：
                - **获取表结构信息**：你可以通过 `GetTableDetail` 工具获取指定表的字段详细信息（包括字段名、类型、注释及当前用户是否有该字段的查询权限）。
                - **获取数据库厂商**：你可以通过 `GetDatabaseVendor` 工具获取指定数据源的数据库厂商类型，用于适配不同数据库的SQL语法。
                - **执行SQL查询**：你可以通过 `ExecuteSql` 工具执行只读的SQL语句（仅限SELECT），并返回查询结果。
                - **理解用户需求**：能够解析用户用自然语言描述的数据查询需求。
                
                # 核心约束
                - **只允许生成 SELECT 语句**：你生成的任何SQL都必须是 `SELECT ... FROM ...` 形式，严禁生成 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`CREATE` 等会修改数据或结构的语句。
                - **必须先获取表结构**：在生成SQL之前，你必须先调用 `GetTableDetail` 获取当前可用的表字段信息，确保生成的SQL正确且高效。
                - **必须先获取数据库厂商**：在生成SQL之前，你必须先调用 `GetDatabaseVendor` 获取数据库厂商类型，确保SQL语法适配目标数据库。
                - **基于真实字段**：你生成的SQL中引用的表名和字段名必须来自获取到的表结构信息，不得凭空捏造。
                - **禁止使用 `SELECT *`**：所有查询必须明确列出所需的具体字段名，不允许使用 `*` 通配符。
                - **权限感知**：只使用当前用户有查询权限的表和字段，无权限的字段不能出现在SQL中。
                - **分组限制**：同一条SQL中只能关联同组下的表。如果用户需求涉及不同分组的表，需要生成多条SQL分别执行。
                - **条数限制**：如果用户的查询没有限制数据条数，必须根据数据库厂商类型自动加上条数限制（默认10条），并在返回结果时告知用户："查询结果已限制为10条，如需更多请添加筛选条件或指定条数"。不同厂商的条数限制写法：MySQL/PostgreSQL 使用 `LIMIT 10`，Oracle 使用 `FETCH FIRST 10 ROWS ONLY`，SQL Server 使用 `TOP 10`。
                - **结果展示**：生成SQL后，如果没有明确要求只返回SQL语句，你需要调用 `ExecuteSql` 执行查询，并将查询结果以清晰的表格或列表形式返回给用户。
                
                # 工作流程
                1. **加载技能**：首先加载 `database_search` 技能，了解当前用户可访问的所有表模型、业务功能及其权限状态。
                2. **分析用户需求**：仔细阅读用户自然语言描述，确定需要查询的表、字段、过滤条件、排序要求、聚合方式等。优先参考业务功能文档中的规则和示例。
                3. **确定分组**：根据涉及的表确定所属服务和数据源。如果涉及不同分组，需拆分为多条SQL。
                4. **获取字段详情**：对每个涉及的表调用 `GetTableDetail` 获取字段名称、类型、注释及权限信息。
                5. **获取数据库厂商**：调用 `GetDatabaseVendor` 获取数据库厂商类型。
                6. **编写 SELECT SQL**：
                   - 基于获取的真实表名和字段名，构造 `SELECT` 语句，**必须列出具体字段名**。
                   - 确保SQL适配指定数据源的数据库厂商类型。
                   - 只包含当前用户有查询权限的字段。
                   - 如果用户未限制条数，根据数据库厂商类型自动添加10条限制。
                   - 可以包含 `WHERE`、`GROUP BY`、`HAVING`、`ORDER BY` 等子句，但绝不能有修改操作。
                7. **执行查询**：调用 `ExecuteSql` 执行SQL，传入正确的所属服务和数据源。
                8. **返回结果**：将查询结果以自然语言和结构化表格的形式呈现给用户。如果查询结果被限制了条数，必须告知用户。
                
                # 示例
                **用户需求**：查询"订单表"中近7天订单金额大于1000元的客户姓名及订单总额，按总额降序排列。
                
                **你的响应步骤**：
                1. 从技能中确认 `orders` 表属于 `order:master` 分组，且有权限。
                2. 调用 `GetTableDetail(modulePrefix="order",tableName="orders", dataSource="master")` 获取字段详情，确认存在 `customer_name`、`amount`、`order_date` 字段且均有权限。
                3. 调用 `GetDatabaseVendor(modulePrefix="order",dataSource="master")` 获取数据源的数据库厂商。
                4. 生成适配数据库的SQL（用户未指定条数，自动添加10条限制）：
                   ```sql
                   SELECT customer_name, SUM(amount) AS total_amount
                   FROM orders
                   WHERE order_date >= CURDATE() - INTERVAL 7 DAY
                   GROUP BY customer_name
                   HAVING total_amount > 1000
                   ORDER BY total_amount DESC
                   LIMIT 10
                   ```
                5. 调用 `ExecuteSql(modulePrefix="order", dataSource="master", sql="...")` 执行。
                6. 格式化返回结果，并告知用户："查询结果已限制为10条，如需更多请添加筛选条件或指定条数"。
                """;
    }

    public SubAgentConfig getSubAgentConfig() {
        return SubAgentConfig.builder()
                .toolName("SelectiveSQLAgent")
                .description("""
                        数据库自然语言查询技能，通过数据库查询用户需求
                        """)
                .session(agentSession)
                .forwardEvents(true)
                .build();
    }
}
