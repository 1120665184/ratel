package org.quyq.gwsu.security.brain.service.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.subagent.SubAgentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.model.ModelProvider;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.SessionUtils;
import org.quyq.gwsu.security.brain.service.skill.DatabaseSearchSkillRepository;
import org.quyq.gwsu.security.brain.service.tool.DatabaseSearchTool;
import org.quyq.gwsu.security.role.service.ISecurityRoleTableModelService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityBusinessFunctionService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelTableService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSearchAgent {

    public static final String AGENT_NAME = "SelectiveSQLAgent";

    private static final String TABLE_MODEL_PERMISSION_KEY = "tableModelPermission";

    private final ObjectProvider<Toolkit> toolkitProvider;

    private final AgentStateStore agentStateStore;

    private final SecurityUtils securityUtils;

    private final SessionUtils sessionUtils;

    private final ISecurityTableModelTableService tableModelTableService;

    private final ISecurityRoleTableModelService roleTableModelService;

    private final ISecurityBusinessFunctionService businessFunctionService;

    private final DatabaseSearchTool databaseSearchTool;

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
        Toolkit toolkit = toolkitProvider.getIfAvailable(Toolkit::new);
        registerDatabaseSearchSkill(toolkit);

        return ReActAgent.builder()
                .name(AGENT_NAME)
                .sysPrompt(buildSystemPrompt())
                .stateStore(agentStateStore)
                .model(ModelProvider.generateModel())
                .toolkit(toolkit)
                .skillRepository(new DatabaseSearchSkillRepository(
                        tableModelTableService::listAll,
                        businessFunctionService::listAll,
                        businessFunctionService::getDetailById,
                        this::getUserTableModelPermission,
                        DeployUtils::isSingle))
                .middleware(new ExecutionContextInjection(this))
                .build();
    }

    private void registerDatabaseSearchSkill(Toolkit toolkit) {
        SkillBox skillBox = new SkillBox(toolkit);
        AgentSkill templateSkill = AgentSkill.builder()
                .name(DatabaseSearchSkillRepository.SKILL_NAME)
                .source("database-search")
                .description("""
                        根据自然语言问题生成SQL并执行查询。优先识别用户是否命中已有业务功能，若命中则优先查阅业务文档与表模型说明，再结合工具返回的真实结构生成只读 SQL。
                        """)
                .skillContent("动态技能占位，不直接使用此内容。")
                .build();

        skillBox.registration()
                .skill(templateSkill)
                .tool(databaseSearchTool)
                .apply();
    }

    private record ExecutionContextInjection(DatabaseSearchAgent agent) implements MiddlewareBase {


        @Override
        public Flux<AgentEvent> onActing(Agent agent, RuntimeContext ctx, ActingInput input, Function<ActingInput, Flux<AgentEvent>> next) {
            ctx.put(DatabaseSearchAgent.class, this.agent);
            return MiddlewareBase.super.onActing(agent, ctx, input, next);
        }
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
                .toolName(AGENT_NAME)
                .description("""
                        数据库自然语言查询技能，通过数据库查询用户需求
                        """)
                .forwardEvents(true)
                .build();
    }
}
