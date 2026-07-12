package org.quyq.gwsu.security.brain.service.impl;


import io.agentscope.core.agent.Agent;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.agui.SingletonAgentResolver;
import org.quyq.gwsu.common.ai.agui.processor.AguiRequestProcessor;
import org.quyq.gwsu.common.ai.agui.tool.AskUserQuestionTool;
import org.quyq.gwsu.common.ai.model.ModelProvider;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.SessionUtils;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.brain.service.IBrainService;
import org.quyq.gwsu.security.brain.service.agent.DatabaseSearchAgent;
import org.quyq.gwsu.security.brain.service.agent.OutputViewAgent;
import org.quyq.gwsu.security.brain.service.middleware.ApprovalTipMiddleware;
import org.quyq.gwsu.security.brain.service.middleware.DynamicViewToolFilterMiddleware;
import org.quyq.gwsu.security.brain.service.middleware.ForwardedPropsMiddleware;
import org.quyq.gwsu.security.brain.service.middleware.StatisticsMiddleware;
import org.quyq.gwsu.security.brain.service.skill.UserMenuSkillRepository;
import org.quyq.gwsu.security.brain.service.tool.web.ClickElementTool;
import org.quyq.gwsu.security.brain.service.tool.web.EnterAiModeTool;
import org.quyq.gwsu.security.brain.service.tool.web.WebTool;
import org.quyq.gwsu.security.menu.service.ISecurityMenuService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/22
 * @description
 */
@Service
@RequiredArgsConstructor
public class BrainServiceImpl implements IBrainService {


    private final ObjectProvider<Toolkit> toolkitProvider;

    private final DatabaseSearchAgent databaseSearchAgent;

    private final OutputViewAgent outputViewAgent;

    private final AgentStateStore agentStateStore;

    private final SecurityUtils securityUtils;

    private final SessionUtils sessionUtils;

    private final ISecurityMenuService menuService;

    private final WebTool webTool;

    private final EnterAiModeTool enterAiModeTool;

    private final ClickElementTool clickElementTool;

    private final ObjectMapper objectMapper;

    private static final String PERMISSION_SOURCE = "central-brain";
    private final Object processorInitMonitor = new Object();
    private volatile Agent singletonAgent;
    private volatile AguiRequestProcessor aguiRequestProcessor;

    public Agent buildAgent() {
        Toolkit toolkit = toolkitProvider.getIfAvailable(Toolkit::new);

        toolkit.registerTool(new AskUserQuestionTool());

        registerViewOperationSkill(toolkit);

        return getAgent(toolkit);
    }

    private void registerViewOperationSkill(Toolkit toolkit) {
        SkillBox skillBox = new SkillBox(toolkit);
        AgentSkill templateSkill = AgentSkill.builder()
                .name(UserMenuSkillRepository.SKILL_NAME)
                .source(PERMISSION_SOURCE)
                .description("当需要操作用户可视化界面时，加载此技能查看当前用户可访问的界面，并按需读取页面按钮说明")
                .skillContent("动态技能占位，不直接使用此内容。")
                .build();

        skillBox.registration()
                .skill(templateSkill)
                .tool(webTool)
                .apply();
        skillBox.registration()
                .skill(templateSkill)
                .agentTool(enterAiModeTool)
                .apply();
        skillBox.registration()
                .skill(templateSkill)
                .agentTool(clickElementTool)
                .apply();
    }


    private Agent getAgent(Toolkit toolkit) {
        //内容输出子智能体
        toolkit.registration()
                .subAgent(outputViewAgent::build, outputViewAgent.getSubAgentConfig())
                .apply();
        //数据库智能查询智能体
        toolkit.registration()
                //数据库搜索子智能体
                .subAgent(databaseSearchAgent::build, databaseSearchAgent.getSubAgentConfig())
                .apply();


        return HarnessAgent.builder()
                .name(CoreConstants.Agent.BRAIN_AGENT_NAME)
                .stateStore(agentStateStore)
                .sysPrompt(buildSysPrompt())
                .model(ModelProvider.generateModel())
                .middlewares(List.of(
                        new StatisticsMiddleware(),
                        new DynamicViewToolFilterMiddleware(),
                        new ApprovalTipMiddleware(objectMapper),
                        new ForwardedPropsMiddleware(securityUtils, sessionUtils, objectMapper)))
                .toolkit(toolkit)
                .skillRepository(new UserMenuSkillRepository(
                        PERMISSION_SOURCE,
                        securityUtils::getUsername,
                        () -> menuService.listUserRoutes(MenuOwner.ADMIN)))
                .maxIters(100)
                .toolExecutionConfig(ExecutionConfig.builder()
                        .timeout(Duration.of(10, ChronoUnit.MINUTES))
                        .build())
                //   .enableAgentTracingLog(true)
                .enableMetaTool(true)
                .disableSubagents()
                .disableShellTool()
                .disableMemoryTools()
                .disableFilesystemTools()
                .build();
    }


    private String buildSysPrompt() {

        return """
                # 角色
                你是管理平台的智能助手「中枢大脑」，协助用户完成平台相关问题与任务。
                
                # 核心原则
                1. **基于事实**：所有回答必须基于平台实际数据，禁止编造信息。不知道的如实说不知道，没有权限的如实告知无权限。
                2. **简洁易懂**：用户是业务人员，回复需简洁明了，禁止使用编程专业词汇，禁止泄露工具的实现逻辑。
                
                # 两大核心能力
                你拥有两个获取平台数据的能力，请根据用户需求选择合适的方式：
                
                ## 可视化界面操作
                - 适用场景：数据修改、界面操作类任务
                - 局限性：只能查看界面展示的内容，无法看到未展示的数据
                - 当用户需要修改数据时，必须使用此能力
                - **重要**：对于界面操作类任务，应根据用户问题先导航到对应的界面，读取界面当前展示的内容；如果界面中已有答案或可完成操作，直接执行；只有界面信息确实不足时，才向用户提问。
                
                ## 数据库搜索
                - 适用场景：数据查询、统计分析类任务
                - 局限性：仅支持查询，不支持修改
                - 当用户需求为查询或统计时，优先使用此能力
                
                ## 能力选择原则
                - 修改数据 → 可视化界面操作
                - 查询/统计数据 → 数据库搜索（优先）
                - 上述所有能力均基于当前用户权限构建，当用户在系统中没有相关权限时，如实告知无权限
                
                # 技能使用
                当用户的问题与已注册技能相关时，优先加载对应技能获取信息：
                - 用户询问功能权限、想跳转页面、想执行操作时，加载 system_view_operation 技能查看其拥有的界面
                - 根据技能中的路由地址和功能描述，决定导航到对应页面
                
                # 内容展示原则（重要）
                你拥有专属的AI输出面板，可以给用户输出可视化形式的内容（如图表、表格、统计卡片等）。
                **当需要向用户展示信息时，优先判断是否适合使用内容面板：**
                - **适合使用内容面板的场景**：结构化数据（表格、列表）、统计结果（数值、占比）、趋势对比（折线图、柱状图）、多维度分析、流程示意等。此时应调用前端展示智能体，将数据以可视化组件形式渲染到内容面板。
                - **不适合使用内容面板的场景**：简单的文字问答、操作指引、权限说明、错误提示等。此时直接输出文字回复即可。
                - **调用方式**：当你已经获得需要展示的数据（例如通过数据库搜索查询到的结果），应将数据连同展示标题、描述一并传递给前端展示智能体，由它完成内容面板渲染。**禁止**直接输出大段文字描述表格或统计结果，除非数据确实不适合可视化。
                - **数据缺失处理**：如果你没有获得任何可展示的数据，却认为应该使用内容面板，请先通过数据库搜索获取数据，或向用户询问缺少的信息。
                
                # 用户交互原则（重要）
                - **疑问时先读界面**：当任务涉及可视化界面操作时，必须先导航到用户问题对应的界面，读取界面信息。如果界面已能回答问题或完成操作，直接处理；确实信息不足时，再使用 `AskUserQuestion` 工具提问。
                - **提问规范**：问题应清晰、具体，提供可选的答案选项，帮助用户快速确认。
                - **避免过度提问**：仅在确实必要的信息缺失时提问，不要对明显的问题反复确认。
                
                # 不确定性处理原则
                1. **识别与澄清**：当用户的问题存在歧义、信息不完整，或可能对应多种理解时，你必须主动向用户提出澄清性问题，明确具体意图后再作答（使用 AskUserQuestion 工具）。
                2. **隐含问题的主动发现**：在解决用户明确提出的问题时，如果发现会自然衍生出其他关联问题（例如：查询某订单状态时，发现订单已超时，但用户未询问超时原因及后续操作），应主动提示用户这些潜在需关注的信息，并询问是否需要进一步协助。
                3. **无法确定时的默认动作**：若经过合理尝试仍无法确定用户意图或问题所需的必要信息，应如实告知用户当前信息不足以给出准确答案，并列出缺少的关键信息，引导用户补充。
                4. **不确定性回答规范**：对于不确定的内容，严禁给出肯定性或猜测性的答案。应明确表达“我不确定”、“需要进一步确认”等措辞，并说明原因或建议的核实方式。
                
                # 当前界面信息
                - 界面路由地址：{currentPath}
                - {headlessContent}
                """;
    }


    @Override
    public AguiRequestProcessor buildAguiProcessor() {
        if (aguiRequestProcessor != null) {
            return aguiRequestProcessor;
        }
        synchronized (processorInitMonitor) {
            if (aguiRequestProcessor == null) {
                Agent agent = getOrCreateSingletonAgent();
                aguiRequestProcessor = AguiRequestProcessor.builder()
                        .agentResolver(new SingletonAgentResolver(agent))
                        .config(AguiAdapterConfig.builder()
                                .enableReasoning(true)
                                .build())
                        .build();
            }
            return aguiRequestProcessor;
        }
    }

    private Agent getOrCreateSingletonAgent() {
        if (singletonAgent != null) {
            return singletonAgent;
        }
        synchronized (processorInitMonitor) {
            if (singletonAgent == null) {
                singletonAgent = buildAgent();
            }
            return singletonAgent;
        }
    }

}
