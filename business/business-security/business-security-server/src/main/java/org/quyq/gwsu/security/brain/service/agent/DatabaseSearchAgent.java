package org.quyq.gwsu.security.brain.service.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.session.Session;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.subagent.SubAgentConfig;
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
                .build();
    }

    private AgentSkill buildDatabaseSearchSkill() {
        List<TableModelTableVO> allTables = tableModelTableService.listAll();
        if (CollectionUtils.isEmpty(allTables)) {
            throw new AgentException("表模型未初始化，请联系管理员在'AI表模型管理'中采集所有表模型");
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

        AgentSkill.Builder skillBuilder = AgentSkill.builder()
                .name("database_search")
                .description("""
                        数据库自然语言查询技能，当有以下需求时使用此技能：
                        - 以当前登录用户的权限为基础生成SQL语句
                        - 生成SQL并执行返回结果
                        """)
                .skillContent(skillContent)
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

        StringBuilder sb = new StringBuilder();
        sb.append("# 数据查询技能\n\n");

        sb.append("## 可用业务功能\n\n");
        sb.append("| 业务名称 | 简介 | 详细文档 |\n");
        sb.append("|---------|------|---------|\n");

        List<BusinessFunctionVO> businessFunctions = businessFunctionService.listAll();
        if (!CollectionUtils.isEmpty(businessFunctions)) {
            for (BusinessFunctionVO bf : businessFunctions) {
                String sanitizedName = sanitizeFileName(bf.getName());
                sb.append("| ").append(bf.getName())
                        .append(" | ").append(bf.getSummary() != null ? bf.getSummary() : "-")
                        .append(" | [查看详情](reference/").append(sanitizedName).append(".md) |\n");
            }
        } else {
            sb.append("| - | 暂无业务功能配置 | - |\n");
        }
        sb.append("\n");

        sb.append("## 数据库表模型概览\n\n");
        sb.append("[查看表模型概览](reference/table_overview.md)\n\n");

        sb.append("## 部署模式\n\n");
        sb.append("当前为**").append(deployMode).append("**模式：\n");
        sb.append("- 单应用部署：按**数据源**分组，同一数据源下的表可以在一条SQL中关联查询\n");
        sb.append("- 微服务部署：按**服务名+数据源**分组，同一服务同一数据源下的表才可以在一条SQL中关联查询\n\n");
        sb.append("当前分组条件：**").append(groupCondition).append("**下的表可以关联查询\n\n");

        sb.append("## 重要约束\n\n");
        sb.append("- 同一条SQL中只能关联**").append(groupCondition).append("**下的表，不同分组的表无法在一条SQL中关联查询\n");
        sb.append("- 如果用户需求涉及不同分组的表，需要生成多条SQL分别执行\n");
        sb.append("- 生成SQL前必须先调用 `GetTableDetail` 工具获取表的详细字段信息和权限\n");
        sb.append("- 生成SQL前必须先调用 `GetDatabaseVendor` 工具获取数据库厂商类型，根据厂商类型使用对应的SQL语法\n");
        sb.append("- 仅允许生成SELECT语句，禁止任何修改操作\n");
        sb.append("- 禁止使用 `SELECT *`，必须明确列出字段名\n");
        sb.append("- 无权限的表和字段不能在SQL中使用\n");
        sb.append("- 如果用户的查询没有限制数据条数，必须根据数据库厂商类型自动加上条数限制（默认10条），并在返回结果时告知用户：\"查询结果已限制为10条，如需更多请添加筛选条件或指定条数\"。不同厂商的条数限制写法：MySQL/PostgreSQL 使用 `LIMIT 10`，Oracle 使用 `FETCH FIRST 10 ROWS ONLY`，SQL Server 使用 `TOP 10`\n\n");

        sb.append("## 使用说明\n\n");
        sb.append("1. 先根据用户问题识别涉及的业务功能，阅读对应的业务详细文档\n");
        sb.append("2. 根据部署模式确定分组条件，判断涉及的表是否属于同一分组\n");
        sb.append("3. 使用 `GetTableDetail` 工具查询涉及表的详细字段信息和权限\n");
        sb.append("4. 使用 `GetDatabaseVendor` 工具获取数据库厂商类型，确定SQL语法\n");
        sb.append("5. 根据业务文档中的规则和示例，结合数据库厂商类型构建准确的查询\n");
        sb.append("6. 注意：部分表可能需要特定权限才能访问，请先确认当前用户是否有权限\n");
        sb.append("7. 如果用户未指定查询条数，必须根据数据库厂商类型自动加上条数限制（默认10条），并在返回时告知用户\n");

        return sb.toString();
    }

    private String buildTableOverviewResource(
            Map<String, List<TableModelTableVO>> groupedTables,
            Map<String, Boolean> tablePermissionMap,
            boolean isSingle) {

        Map<String, String> tableBusinessMap = buildTableBusinessMap();

        StringBuilder sb = new StringBuilder();
        sb.append("# 数据库表模型概览\n\n");

        for (Map.Entry<String, List<TableModelTableVO>> entry : groupedTables.entrySet()) {
            if (isSingle) {
                String dataSource = entry.getKey();
                sb.append("## 分组：").append(dataSource).append("（数据源）\n\n");
                sb.append("| 表名 | 表注释 | 所属业务 | 有权限 |\n");
                sb.append("|------|--------|---------|--------|\n");
                for (TableModelTableVO table : entry.getValue()) {
                    String key = table.getModulePrefix() + ":" + table.getDataSource() + ":" + table.getTableName();
                    boolean hasPermission = tablePermissionMap.getOrDefault(key, true);
                    String businesses = tableBusinessMap.getOrDefault(table.getTableName(), "-");
                    sb.append("| ").append(table.getTableName())
                            .append(" | ").append(table.getTableComment() != null ? table.getTableComment() : "-")
                            .append(" | ").append(businesses)
                            .append(" | ").append(hasPermission ? "是" : "否")
                            .append(" |\n");
                }
            } else {
                String[] parts = entry.getKey().split(":", 2);
                String modulePrefix = parts[0];
                String dataSource = parts.length > 1 ? parts[1] : "master";
                sb.append("## 分组：").append(modulePrefix).append(":").append(dataSource).append("（服务:数据源）\n\n");
                sb.append("| 表名 | 表注释 | 所属业务 | 有权限 |\n");
                sb.append("|------|--------|---------|--------|\n");
                for (TableModelTableVO table : entry.getValue()) {
                    String key = table.getModulePrefix() + ":" + table.getDataSource() + ":" + table.getTableName();
                    boolean hasPermission = tablePermissionMap.getOrDefault(key, true);
                    String businesses = tableBusinessMap.getOrDefault(table.getTableName(), "-");
                    sb.append("| ").append(table.getTableName())
                            .append(" | ").append(table.getTableComment() != null ? table.getTableComment() : "-")
                            .append(" | ").append(businesses)
                            .append(" | ").append(hasPermission ? "是" : "否")
                            .append(" |\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
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

            sb.append("## 业务简介\n");
            sb.append(bf.getSummary() != null ? bf.getSummary() : "-").append("\n\n");

            if (bf.getDetail() != null && !bf.getDetail().isBlank()) {
                sb.append("## 详细介绍\n\n");
                sb.append(bf.getDetail()).append("\n\n");
            }

            if (detail != null && !CollectionUtils.isEmpty(detail.getTables())) {
                sb.append("## 关联表模型\n\n");
                sb.append("| 表名 | 表注释 | 当前用户权限 |\n");
                sb.append("|------|--------|-------------|\n");
                for (TableModelTableVO table : detail.getTables()) {
                    String key = table.getModulePrefix() + ":" + table.getDataSource() + ":" + table.getTableName();
                    boolean hasPermission = tablePermissionMap.getOrDefault(key, true);
                    sb.append("| ").append(table.getTableName())
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
