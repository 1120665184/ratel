package org.quyq.gwsu.security.brain.service.agent;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallExecutionContext;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.ContextStore;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.ToolExecutionContextProvider;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.subagent.SubAgentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.SessionUtils;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelTableVO;
import org.quyq.gwsu.security.brain.service.tool.DatabaseSearchTool;
import org.quyq.gwsu.security.role.service.ISecurityRoleTableModelService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelTableService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 自然语言生成SQL与执行智能体
 * 基于AgentScope的ReActAgent实现，具备表模型权限感知能力
 *
 * @author Quyq
 * @date 2026/5/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSearchAgent {

    private final ObjectProvider<Memory> memoryProvider;

    private final ObjectProvider<Toolkit> toolkitProvider;

    private final Session agentSession;

    private final Model model;

    private final SecurityUtils securityUtils;

    private final SessionUtils sessionUtils;

    private final ISecurityTableModelTableService tableModelTableService;

    private final ISecurityRoleTableModelService roleTableModelService;

    private final DatabaseSearchTool databaseSearchTool;

    private final static String TABLE_MODEL_PERMISSION_KEY = "tableModelPermission";


    /**
     * 获取当前登录用户的表模型权限
     *
     * @return
     */
    public Map<String, Map<String, FieldPermission>> getUserTableModelPermission() {
        Optional<Map<String, Map<String, FieldPermission>>> value = sessionUtils.getValue(TABLE_MODEL_PERMISSION_KEY);
        if (value.isPresent()) {
            return value.get();
        }
        // 获取当前用户的角色
        List<String> roles = securityUtils.getSubject()
                .map(Subject::getRoles)
                .orElse(Collections.emptyList());

        // 获取当前用户合并后的表模型权限
        Map<String, Map<String, FieldPermission>> mergedPermissions = Map.of();
        if (!CollectionUtils.isEmpty(roles)) {
            mergedPermissions = roleTableModelService.getMergedRoleTableModelPermission(roles);
        }

        sessionUtils.putValue(TABLE_MODEL_PERMISSION_KEY, mergedPermissions);

        return mergedPermissions;
    }


    /**
     * 构建智能体
     */
    public ReActAgent build() {
        Memory memory = memoryProvider.getIfAvailable();
        Toolkit toolkit = toolkitProvider.getIfAvailable(Toolkit::new);

        // 注册工具
        // toolkit.registerTool(databaseSearchTool);

        // 构建技能
        AgentSkill skill = buildDatabaseSearchSkill();

        // 使用SkillBox注册技能，并将工具与技能关联
        SkillBox skillBox = new SkillBox(toolkit);
        skillBox.registration()
                .skill(skill)
                .tool(databaseSearchTool)
                .apply();

        return ReActAgent.builder()
                .name("SelectiveSQLAgent")
                .sysPrompt(buildSystemPrompt())
                .memory(memory)
                .model(model)
                .toolkit(toolkit)
                .skillBox(skillBox)
                .toolExecutionContext(ToolExecutionContext.builder()
                        .register(DatabaseSearchAgent.class, this)
                        .build())
                .build();
    }

    /**
     * 构建数据库查询技能
     * 技能中包含当前用户拥有的所有表模型信息，按所属服务和数据源分组
     */
    private AgentSkill buildDatabaseSearchSkill() {
        String skillContent = buildSkillContent();

        return AgentSkill.builder()
                .name("database_search")
                .description("""
                        数据库自然语言查询技能，当有以下需求时使用此技能：
                        - 以当前登录用户的权限为基础生成SQL语句
                        - 生成SQL并执行返回结果
                        """)
                .skillContent(skillContent)
                .build();
    }


    /**
     * 构建技能内容
     * 列出当前用户拥有的表模型信息，按所属服务和数据源分组
     */
    private String buildSkillContent() {
        // 获取所有表模型
        List<TableModelTableVO> allTables = tableModelTableService.listAll();
        if (CollectionUtils.isEmpty(allTables)) {
            return "# 数据库查询技能\n\n当前无可用的表模型信息。";
        }

        // 获取当前用户合并后的表模型权限
        Map<String, Map<String, FieldPermission>> mergedPermissions = getUserTableModelPermission();


        // 判断每个表当前用户是否有权限（至少有一个字段show=true）
        Map<String, Boolean> tablePermissionMap = new LinkedHashMap<>();
        for (TableModelTableVO table : allTables) {
            String key = table.getModulePrefix() + ":" + table.getDataSource() + ":" + table.getTableName();
            Map<String, FieldPermission> fieldPerms = mergedPermissions.get(key);

            tablePermissionMap.put(key, Objects.nonNull(fieldPerms));
        }

        // 按所属服务+数据源分组
        Map<String, List<TableModelTableVO>> groupedTables;
        String groupCondition;
        //单应用按照数据源分组 ， 微服务按照服务名+数据源分组
        if (DeployUtils.isSingle()) {
            groupedTables = allTables.stream()
                    .collect(Collectors.groupingBy(
                            TableModelTableVO::getDataSource,
                            LinkedHashMap::new,
                            Collectors.toList()));
            groupCondition = "同一数据源";
        } else {
            groupedTables = allTables.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getModulePrefix() + ":" + t.getDataSource(),
                            LinkedHashMap::new,
                            Collectors.toList()));
            groupCondition = "同一服务同一数据源";
        }

        // 构建技能文档
        StringBuilder sb = new StringBuilder();
        sb.append("# 数据库自然语言查询技能\n\n");

        sb.append("## 重要约束\n");
        sb.append("- 同一条SQL中只能关联**%s**下的表，不同分组的表无法在一条SQL中关联查询\n".formatted(groupCondition));
        sb.append("- 如果用户需求涉及不同分组的表，需要生成多条SQL分别执行\n");
        sb.append("- 生成SQL前必须先调用 `GetTableDetail` 工具获取表的详细字段信息和权限\n");
        sb.append("- 仅允许生成SELECT语句，禁止任何修改操作\n");
        sb.append("- 禁止使用 `SELECT *`，必须明确列出字段名\n");
        sb.append("- 无权限的表和字段不能在SQL中使用\n\n");

        sb.append("## 可用表模型列表\n\n");

        for (Map.Entry<String, List<TableModelTableVO>> entry : groupedTables.entrySet()) {
            String modulePrefix;
            String dataSource;
            if (DeployUtils.isSingle()) {
                dataSource = entry.getKey();
                modulePrefix = entry.getValue().getFirst().getModulePrefix();
            } else {
                String[] parts = entry.getKey().split(":", 2);
                modulePrefix = parts[0];
                dataSource = parts.length > 1 ? parts[1] : "master";
            }


            sb.append("### 服务: ").append(modulePrefix)
                    .append(" | 数据源: ").append(dataSource).append("\n\n");

            sb.append("| 表名 | 表注释 | 有权限 |\n");
            sb.append("|------|--------|--------|\n");

            for (TableModelTableVO table : entry.getValue()) {
                String key = table.getModulePrefix() + ":" + table.getDataSource() + ":" + table.getTableName();
                boolean hasPermission = tablePermissionMap.getOrDefault(key, true);

                sb.append("| ").append(table.getTableName())
                        .append(" | ").append(table.getTableComment() != null ? table.getTableComment() : "-")
                        .append(" | ").append(hasPermission ? "是" : "否")
                        .append(" |\n");
            }
            sb.append("\n");
        }

        sb.append("## 使用流程\n");
        sb.append("1. 根据用户需求，从上方列表中确定涉及的表及其所属服务/数据源\n");
        sb.append("2. 如果涉及多个分组的表，需要拆分为多条SQL\n");
        sb.append("3. 对每个需要查询的表，调用 `GetTableDetail` 获取字段详情和权限\n");
        sb.append("4. 基于字段信息生成SELECT语句（只包含有权限的字段）\n");
        sb.append("5. 调用 `ExecuteSql` 执行SQL，传入正确的所属服务和数据源\n");

        return sb.toString();
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return """
                # 角色定义
                你是一个专业的SQL查询生成助手，拥有以下能力：
                - **获取表结构信息**：你可以通过 `GetTableDetail` 工具获取指定表的字段详细信息（包括字段名、类型、注释及当前用户是否有该字段的查询权限）。
                - **执行SQL查询**：你可以通过 `ExecuteSql` 工具执行只读的SQL语句（仅限SELECT），并返回查询结果。
                - **理解用户需求**：能够解析用户用自然语言描述的数据查询需求。
                
                # 核心约束
                - **只允许生成 SELECT 语句**：你生成的任何SQL都必须是 `SELECT ... FROM ...` 形式，严禁生成 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`CREATE` 等会修改数据或结构的语句。
                - **必须先获取表结构**：在生成SQL之前，你必须先调用 `GetTableDetail` 获取当前可用的表字段信息，确保生成的SQL正确且高效。
                - **基于真实字段**：你生成的SQL中引用的表名和字段名必须来自获取到的表结构信息，不得凭空捏造。
                - **禁止使用 `SELECT *`**：所有查询必须明确列出所需的具体字段名，不允许使用 `*` 通配符。
                - **权限感知**：只使用当前用户有查询权限的表和字段，无权限的字段不能出现在SQL中。
                - **分组限制**：同一条SQL中只能关联同组下的表。如果用户需求涉及不同分组的表，需要生成多条SQL分别执行。
                - **结果展示**：生成SQL后，如果没有明确要求只返回SQL语句，你需要调用 `ExecuteSql` 执行查询，并将查询结果以清晰的表格或列表形式返回给用户。
                
                # 工作流程
                1. **加载技能**：首先加载 `database_search` 技能，了解当前用户可访问的所有表模型及其权限状态。
                2. **分析用户需求**：仔细阅读用户自然语言描述，确定需要查询的表、字段、过滤条件、排序要求、聚合方式等。
                3. **确定分组**：根据涉及的表确定所属服务和数据源。如果涉及不同分组，需拆分为多条SQL。
                4. **获取字段详情**：对每个涉及的表调用 `GetTableDetail` 获取字段名称、类型、注释及权限信息。
                5. **获取数据库厂商**：调用`GetDatabaseVendor`获取数据库厂商类型。
                5. **编写 SELECT SQL**：
                   - 基于获取的真实表名和字段名，构造 `SELECT` 语句，**必须列出具体字段名**。
                   - 确保SQL适配指定数据源的数据库厂商类型。
                   - 只包含当前用户有查询权限的字段。
                   - 可以包含 `WHERE`、`GROUP BY`、`HAVING`、`ORDER BY`、`LIMIT` 等子句，但绝不能有修改操作。
                6. **执行查询**：调用 `ExecuteSql` 执行SQL，传入正确的所属服务和数据源。
                7. **返回结果**：将查询结果以自然语言和结构化表格的形式呈现给用户。如果查询失败，给出明确的错误提示。
                
                # 示例
                **用户需求**：查询"订单表"中近7天订单金额大于1000元的客户姓名及订单总额，按总额降序排列。
                
                **你的响应步骤**：
                1. 从技能中确认 `orders` 表属于 `order:master` 分组，且有权限。
                2. 调用 `GetTableDetail(modulePrefix="order",tableName="orders", dataSource="master")` 获取字段详情，确认存在 `customer_name`、`amount`、`order_date` 字段且均有权限。
                3. 生成SQL：
                   ```sql
                   SELECT customer_name, SUM(amount) AS total_amount
                   FROM orders
                   WHERE order_date >= CURDATE() - INTERVAL 7 DAY
                   GROUP BY customer_name
                   HAVING total_amount > 1000
                   ORDER BY total_amount DESC
                   ```
                4. 调用 `ExecuteSql(modulePrefix="order", dataSource="master", sql="...")` 执行。
                5. 格式化返回结果。
                """;
    }

    /**
     * 获取子智能体配置（供其他智能体调用）
     */
    public SubAgentConfig getSubAgentConfig() {
        return SubAgentConfig.builder()
                .toolName("SelectiveSQLAgent")
                .description("""
                        数据库自然语言查询技能，当有以下需求时使用此技能：
                        - 以当前登录用户的权限为基础生成SQL语句（仅支持查询语句）
                        - 生成SQL并执行返回结果
                        """)
                .session(agentSession)
                .forwardEvents(true)
                .build();
    }
}
