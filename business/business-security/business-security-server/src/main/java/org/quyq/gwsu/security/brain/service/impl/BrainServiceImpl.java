package org.quyq.gwsu.security.brain.service.impl;


import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.processor.AguiRequestProcessor;
import io.agentscope.core.agui.registry.AguiAgentRegistry;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.agui.DefaultAgentResolver;
import org.quyq.gwsu.common.ai.agui.ThreadSessionManager;
import org.quyq.gwsu.common.ai.agui.tool.AskUserQuestionTool;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.security.brain.service.IBrainService;
import org.quyq.gwsu.security.brain.service.tool.WebTool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * @author Quyq
 * @date 2026/4/22
 * @description
 */
@Service
@RequiredArgsConstructor
public class BrainServiceImpl implements IBrainService {

    private final ObjectProvider<Memory> memoryProvider;

    private final ObjectProvider<Toolkit> toolkitProvider;

    private final Session agentSession;

    private final Model model;

    private final SecurityUtils securityUtils;

    private final WebToolUtils webToolUtils;


    public Agent buildAgent() {
        Memory memory = memoryProvider.getIfAvailable();
        Toolkit toolkit = toolkitProvider.getIfAvailable(Toolkit::new);


        toolkit.registerTool(new WebTool(webToolUtils));
        toolkit.registerTool(new AskUserQuestionTool());

        return getAgent(memory, toolkit);
    }


    private ReActAgent getAgent(Memory memory, Toolkit toolkit) {
        return ReActAgent.builder()
                .name("CentralBrain")
                .sysPrompt(buildSysPrompt())
                .model(model)
                .memory(memory)
                .toolkit(toolkit)
                .build();
    }

    private String buildSysPrompt() {
        return """
                # 角色定义
                你是管理平台的智能助手「中枢大脑」，专注于协助用户完成平台管理与业务操作。
                
                
                # 行为准则
                1. **精准响应**：理解用户意图，给出准确、简洁的回答
                2. **安全意识**：涉及权限、安全配置时，提醒用户注意安全影响
                3. **操作指引**：需要用户操作时，提供清晰的步骤说明
                4. **边界意识**：超出平台范围的问题，礼貌说明能力边界
                
                # 交互风格
                - 使用中文，语气专业友好
                - 复杂问题分步骤解答，必要时使用列表或表格
                - 技术术语首次出现时附带简要说明
                
                # 工具使用
                根据用户请求类型，选择合适的工具执行操作：
                - 查询类请求：使用查询工具获取数据
                - 操作类请求：确认用户意图后执行，并反馈结果
                - 咨询类请求：直接回答，无需调用工具
                """;
    }

    @Override
    public AguiRequestProcessor buildAguiProcessor() {

        AguiAgentRegistry registry = new AguiAgentRegistry();
        registry.registerFactory(IBrainService.AGENT_ID, this::buildAgent);


        return AguiRequestProcessor.builder()
                .agentResolver(
                        DefaultAgentResolver.builder()
                                .registry(registry)
                                .sessionManager(new ThreadSessionManager(1000, 30))
                                .serverSideMemory(true)
                                .session(agentSession)
                                .getUserIdSupplier(() -> securityUtils.userInfo().map(UserInfo::getUserId).orElse(null))
                                .build()
                )
                .config(AguiAdapterConfig.defaultConfig())
                .build();
    }
}
