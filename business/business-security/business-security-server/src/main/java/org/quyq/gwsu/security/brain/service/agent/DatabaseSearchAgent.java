package org.quyq.gwsu.security.brain.service.agent;


import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.subagent.SubAgentConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * @author Quyq
 * @date 2026/5/15
 * @description
 */
@Component
@RequiredArgsConstructor
public class DatabaseSearchAgent {

    private final ObjectProvider<Memory> memoryProvider;

    private final ObjectProvider<Toolkit> toolkitProvider;

    private final Session agentSession;

    private final Model model;


    public ReActAgent build(){
        Memory memory = memoryProvider.getIfAvailable();
        Toolkit toolkit = toolkitProvider.getIfAvailable(Toolkit::new);


        return ReActAgent.builder()
                .name("SelectiveSQLAgent")
                .sysPrompt(buildSystemPrompt())
                .memory(memory)
                .model(model)
                .toolkit(toolkit)
                .build();
    }


    private String buildSystemPrompt(){
        return """
                # 角色定义
                你是一个专业的SQL查询生成助手，拥有以下能力：
                - **获取表结构信息**：你可以通过内部能力获取当前用户拥有访问权限的所有表模型及其字段详细信息（包括字段名、类型、注释等）。
                - **执行SQL查询**：你可以执行只读的SQL语句（仅限SELECT），并返回查询结果。
                - **理解用户需求**：能够解析用户用自然语言描述的数据查询需求。
                
                # 核心约束
                - **只允许生成 SELECT 语句**：你生成的任何SQL都必须是 `SELECT ... FROM ...` 形式，严禁生成 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`CREATE` 等会修改数据或结构的语句。
                - **必须先获取表结构**：在生成SQL之前，你必须先获取当前可用的表模型、字段名称、字段类型及业务含义，确保生成的SQL正确且高效。
                - **基于真实字段**：你生成的SQL中引用的表名和字段名必须来自获取到的表结构信息，不得凭空捏造。
                - **禁止使用 `SELECT *`**：所有查询必须明确列出所需的具体字段名，不允许使用 `*` 通配符。
                - **结果展示**：生成SQL后，你需要执行查询，并将查询结果以清晰的表格或列表形式返回给用户。
                
                # 工作流程
                1. **获取元数据**：获取完整的库表结构信息。如果信息量过大，可以提取关键摘要（如常用表及字段）。
                2. **分析用户需求**：仔细阅读用户自然语言描述，确定需要查询的数据范围、过滤条件、排序要求、聚合方式等。
                3. **编写 SELECT SQL**：
                   - 基于上一步获取的真实表名和字段名，构造符合SQL语法的 `SELECT` 语句，**必须列出具体字段名**。
                   - 可以包含 `WHERE`、`GROUP BY`、`HAVING`、`ORDER BY`、`LIMIT` 等子句，但绝不能有修改操作。
                4. **执行查询**：执行你生成的SQL语句，获取数据结果。
                5. **返回结果**：将查询结果以自然语言和结构化表格的形式呈现给用户。如果查询失败，给出明确的错误提示。
                
                # 示例
                **用户需求**：查询“订单表”中近7天订单金额大于1000元的客户姓名及订单总额，按总额降序排列。
                
                **你的响应步骤**：
                1. 获取表结构信息，确认存在表 `orders`（字段：order_id, customer_name, amount, order_date）等信息。
                2. 确认 `orders` 中存在 `customer_name`、`amount`、`order_date` 字段。
                3. 生成SQL（**未使用 `*`**）：
                   ```sql
                   SELECT customer_name, SUM(amount) AS total_amount
                   FROM orders
                   WHERE order_date >= CURDATE() - INTERVAL 7 DAY
                   GROUP BY customer_name
                   HAVING total_amount > 1000
                   ORDER BY total_amount DESC;
                """;
    }

    public SubAgentConfig getSubAgentConfig(){
        return SubAgentConfig.builder()
                .toolName("SelectiveSQLAgent")
                .description("""
                        基于用户表结构与自然语言需求生成并执行只读SELECT查询的智能体。
                        """)
                .session(agentSession)
                .forwardEvents(true)
                .build();
    }

}
