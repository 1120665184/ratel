package org.quyq.gwsu.security.brain.service.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.subagent.SubAgentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.AgentException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 视图输出智能体
 * 将回复内容以精美的可视化界面展示给用户
 * 输出 json-render spec，通过 AGENT_OUTPUT 自定义事件发送到前端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutputViewAgent {

    public static final String AGENT_NAME = "OutputViewAgent";

    private static final String SKILL_RESOURCE_PATH = "skills";

    private final ObjectProvider<Memory> memoryProvider;

    private final ObjectProvider<Toolkit> toolkitProvider;

    private final Session agentSession;

    private final Model model;

    /**
     * 构建视图输出智能体
     */
    public Agent build() {
        Memory memory = memoryProvider.getIfAvailable();
        Toolkit toolkit = toolkitProvider.getIfAvailable(Toolkit::new);

        // 构建技能盒子
        SkillBox skillBox = buildSkillBox(toolkit);

        return ReActAgent.builder()
                .name(AGENT_NAME)
                .sysPrompt(buildSystemPrompt())
                .memory(memory)
                .model(model)
                .toolkit(toolkit)
                .skillBox(skillBox)
                .build();
    }

    /**
     * 构建技能盒子
     * 注册 output_view 技能，包含组件目录和 spec 格式规范
     */
    private SkillBox buildSkillBox(Toolkit toolkit) {
        SkillBox skillBox = new SkillBox(toolkit);


        AgentSkill skill;
        try (ClasspathSkillRepository repository = new ClasspathSkillRepository(SKILL_RESOURCE_PATH)) {
            skill = repository.getSkill("output-view");
        } catch (IOException e) {
            throw new AgentException(e);
        }


        skillBox.registration()
                .skill(skill)
                .apply();

        return skillBox;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return """
                # 角色
                你是一个专业的数据可视化输出智能体。你的任务是将信息以精美的可视化界面展示给用户。

                # 工作流程
                1. 分析用户需要展示的内容类型
                2. 加载 output_view 技能，了解支持的组件和输出格式
                3. 根据内容选择合适的组件
                4. 输出 JSONL 格式的 JSON Patch 操作

                # 输出格式（极其重要）
                你必须输出 JSONL 格式（每行一个 JSON 对象），使用 RFC 6902 JSON Patch 操作：
                - 第一行设置 root：{"op":"add","path":"/root","value":"<key>"}
                - 后续行逐个添加元素：{"op":"add","path":"/elements/<key>","value":{"type":"组件名","props":{...},"children":[...]}}

                严禁输出以下格式：
                - 嵌套的 JSON 树结构（如 {"type":"Dashboard","sections":[...]}）
                - markdown 代码块（如 ```json ... ```）
                - 任何 JSON 之外的额外文字说明

                正确示例：
                {"op":"add","path":"/root","value":"d1"}
                {"op":"add","path":"/elements/d1","value":{"type":"Dashboard","props":{"title":"示例"},"children":["s1"]}}
                {"op":"add","path":"/elements/s1","value":{"type":"Section","props":{"title":"指标","layout":"row"},"children":["c1"]}}
                {"op":"add","path":"/elements/c1","value":{"type":"StatCard","props":{"title":"总数","value":"100"},"children":[]}}

                # 可用组件
                只能使用以下组件：Dashboard、Section、StatCard、Chart、DataTable、TextBlock、FlowChart
                不要使用任何不在此列表中的组件名。

                # 布局规则
                - 必须以 Dashboard 作为根元素
                - 使用 Section 分组：layout="row" 并排展示，layout="column" 垂直排列
                - 叶子组件（StatCard、Chart、DataTable、TextBlock、FlowChart）的 children 为空数组 []

                # 完整性规则
                - 引用子元素前必须先添加该子元素
                - 如果元素有 children: ['a', 'b']，则元素 a 和 b 必须存在
                """;
    }


    /**
     * 获取子智能体配置（供其他智能体调用）
     */
    public SubAgentConfig getSubAgentConfig() {
        return SubAgentConfig.builder()
                .toolName(AGENT_NAME)
                .description("""
                        将回复内容以漂亮的UI形式展示给用户，调用后内容会直接渲染到用户界面，无需你再重复输出相同内容。
                        以下场景适合调用：
                        - 数据统计展示
                        - 数据分析报表展示
                        - 流程图展示
                        - 对比分析展示
                        注意：调用此工具后，内容已直接在可视化面板中展示给用户，你不需要再以文字形式重复输出相同信息。
                        """)
                .session(agentSession)
                .forwardEvents(true)
                .build();
    }
}
