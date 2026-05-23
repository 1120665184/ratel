package org.quyq.gwsu.security.brain.service.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.subagent.SubAgentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    private static final String SKILL_RESOURCE_PATH = "output-view/skill.md";

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

        String skillContent = loadResource(SKILL_RESOURCE_PATH);

        AgentSkill outputViewSkill = AgentSkill.builder()
                .name("output_view")
                .description("将回复内容以可视化界面展示给用户时加载此技能，包含支持的组件列表和 spec 格式规范")
                .skillContent(skillContent)
                .build();

        skillBox.registration()
                .skill(outputViewSkill)
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
                2. 加载 output_view 技能，了解支持的组件和 spec 格式
                3. 根据内容选择合适的组件，阅读对应的详细文档
                4. 输出符合 spec 格式的 JSON

                # 输出要求
                1. 必须输出纯 JSON 格式，不要包含 markdown 代码块标记
                2. 根元素必须是 Dashboard 类型
                3. 使用 Section 对内容进行分组
                4. 合理使用布局：并排展示用 layout="row"，垂直排列用 layout="column"
                5. 选择最合适的组件类型展示数据

                # 重要提示
                - 你输出的 JSON 会被前端流式渲染，请确保格式正确
                - 不要输出任何 JSON 之外的额外文字说明
                - 如果数据量大，优先用 DataTable；如果需要展示趋势，用 Chart
                """;
    }

    /**
     * 从 classpath 加载资源文件内容
     */
    private String loadResource(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            log.error("加载资源文件失败: {}", path, e);
            return "";
        }
    }

    /**
     * 获取子智能体配置（供其他智能体调用）
     */
    public SubAgentConfig getSubAgentConfig() {
        return SubAgentConfig.builder()
                .toolName(AGENT_NAME)
                .description("""
                        将回复内容以漂亮的UI形式展示给用户。
                        以下场景适合调用：
                        - 数据统计展示
                        - 数据分析报表展示
                        - 流程图展示
                        - 对比分析展示
                        """)
                .session(agentSession)
                .forwardEvents(true)
                .build();
    }
}
