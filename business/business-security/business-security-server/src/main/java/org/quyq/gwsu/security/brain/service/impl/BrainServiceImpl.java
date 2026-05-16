package org.quyq.gwsu.security.brain.service.impl;


import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.processor.AguiRequestProcessor;
import io.agentscope.core.agui.registry.AguiAgentRegistry;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.agui.DefaultAgentResolver;
import org.quyq.gwsu.common.ai.agui.ThreadSessionManager;
import org.quyq.gwsu.common.ai.agui.tool.AskUserQuestionTool;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.utils.AIMsgUtils;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.api.menu.vo.MenuVO;
import org.quyq.gwsu.security.brain.service.IBrainService;
import org.quyq.gwsu.security.brain.service.agent.DatabaseSearchAgent;
import org.quyq.gwsu.security.brain.service.tool.WebTool;
import org.quyq.gwsu.security.menu.service.ISecurityMenuService;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.*;

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

    private final DatabaseSearchAgent databaseSearchAgent;

    private final Session agentSession;

    private final Model model;

    private final SecurityUtils securityUtils;

    private final WebToolUtils webToolUtils;

    private final ISecurityMenuService menuService;

    private final WebTool webTool;


    public Agent buildAgent() {
        Memory memory = memoryProvider.getIfAvailable();
        Toolkit toolkit = toolkitProvider.getIfAvailable(Toolkit::new);

        toolkit.registerTool(new AskUserQuestionTool());

        // 构建包含当前用户菜单权限信息的技能
        SkillBox skillBox = buildSkillBox(toolkit);

        return getAgent(memory, toolkit, skillBox);
    }

    /**
     * 构建技能盒子，注册用户菜单权限技能
     * 技能内容直接包含当前用户的菜单和按钮权限信息，
     * AI加载技能后即可直接看到用户拥有的所有功能，无需再调工具查询
     */
    private SkillBox buildSkillBox(Toolkit toolkit) {
        SkillBox skillBox = new SkillBox(toolkit);

        // 查询当前用户的菜单权限，直接嵌入技能内容
        List<MenuVO> menuTree = menuService.listUserRoutes(MenuOwner.ADMIN);
        String menuContent = buildMenuContent(menuTree);

        AgentSkill userMenuSkill = AgentSkill.builder()
                .name("user_menu_permissions")
                .description("当需要操作用户界面,了解用户拥有的功能权限、决定跳转哪个页面、判断用户能执行什么操作时，加载此技能查看用户的完整菜单和功能列表以及操作界面相关工具")
                .skillContent(menuContent)
                .build();

        skillBox.registration()
                .skill(userMenuSkill)
                .tool(webTool)
                .apply();

        return skillBox;
    }

    /**
     * 将菜单树构建为技能内容文本
     */
    private String buildMenuContent(List<MenuVO> menuTree) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 用户拥有的的功能权限信息\n\n");
        sb.append("  以下是当前登录用户拥有的所有菜单、页面和操作按钮信息。");
        sb.append("当用户请求跳转界面或执行操作时，请参考此列表判断用户是否有对应功能，并使用路由地址进行导航。\n\n");

        if (menuTree == null || menuTree.isEmpty()) {
            sb.append("当前用户没有任何菜单权限。\n");
            return sb.toString();
        }

        for (MenuVO menu : menuTree) {
            appendMenuNode(sb, menu, 0);
        }

        sb.append("""
                # 备注
                - 路由：前端视图层界面跳转地址。
                - 位置：菜单在视图层的展示位置。
                - 接口权限：菜单或按钮对应的后端接口权限标识，有(main)标注的接口为对应功能的主要接口，多个权限用`;`分割。
                - 按钮标识：对应按钮的唯一标识，用于判定视图层按钮的显示权限
                """);

        return sb.toString();
    }

    /**
     * 递归构建菜单节点描述
     */
    private void appendMenuNode(StringBuilder sb, MenuVO menu, int depth) {
        String indent = "  ".repeat(depth);
        String typeLabel = switch (menu.getMenuType()) {
            case 1 -> "目录";
            case 2 -> "菜单";
            case 3 -> "按钮";
            default -> "未知";
        };

        sb.append(indent).append("- **").append(menu.getMenuName()).append("**")
                .append(" [").append(typeLabel).append("]");

        if (2 == menu.getMenuType()) {
            sb.append(" 路由: `").append(menu.getPath()).append("`");
        }

        if (Arrays.asList(1, 2).contains(menu.getMenuType())) {
            sb.append(" 位置: ").append(menu.getPosition().getDescription());
        }


        if (StringUtils.hasText(menu.getPermission())) {
            sb.append(" 接口权限: `").append(menu.getPermission()).append("`");
        }

        sb.append("\n");

        // 功能描述
        if (menu.getDescription() != null && !menu.getDescription().isEmpty()) {
            sb.append(indent).append("  > ").append(menu.getDescription()).append("\n");
        }

        // 子菜单
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            List<MenuVO> buttons = menu.getChildren().stream()
                    .filter(child -> child.getMenuType() != null && child.getMenuType() == 3)
                    .toList();
            List<MenuVO> nonButtons = menu.getChildren().stream()
                    .filter(child -> child.getMenuType() == null || child.getMenuType() != 3)
                    .toList();

            // 按钮操作归组
            if (!buttons.isEmpty()) {
                sb.append(indent).append("  操作按钮:\n");
                for (MenuVO btn : buttons) {
                    sb.append(indent).append("    - ").append(btn.getMenuName());
                    if (btn.getDescription() != null && !btn.getDescription().isEmpty()) {
                        sb.append(": ").append(btn.getDescription());
                    }
                    if (StringUtils.hasText(btn.getPermission())) {
                        sb.append(" (接口权限: `").append(btn.getPermission()).append("`  按钮标识：`").append(btn.getButtonKey()).append("`)");
                    }
                    sb.append("\n");
                }
            }

            // 子目录/子菜单递归
            for (MenuVO child : nonButtons) {
                appendMenuNode(sb, child, depth + 1);
            }
        }
    }


    private ReActAgent getAgent(Memory memory, Toolkit toolkit, SkillBox skillBox) {
        //数据库智能查询智能体
        toolkit.registration()
                .subAgent(databaseSearchAgent::build, databaseSearchAgent.getSubAgentConfig())
                .apply();

        return ReActAgent.builder()
                .name("CentralBrain")
                .sysPrompt(buildSysPrompt())
                .model(model)
                .hook(new ForwardedPropsHook())
                .memory(memory)
                .toolkit(toolkit)
                .skillBox(skillBox)
                .maxIters(50)
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
                
                # 技能使用
                当用户的问题与已注册技能相关时，优先加载对应技能获取信息：
                - 用户询问功能权限、想跳转页面、想执行操作时，加载 user_menu_permissions 技能查看其拥有的功能
                - 根据技能中的路由地址和功能描述，决定使用 RouteNavigation 工具导航到对应页面
                
                
                # 当前界面信息
                - 界面路由地址：{currentPath}
                - 界面操作模式(human:人类操作模式 | ai:AI操作模式)：{operationMode}
                
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


    /**
     * 系统提示词动态参数替换
     */
    private static class ForwardedPropsHook implements Hook {

        private final StTemplateRenderer templateRenderer = StTemplateRenderer.builder().build();
        private final static String FORWARDED_PROPS_KEY = "forwardedProps";

        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {

            if (event instanceof PreReasoningEvent preReasoningEvent) {

                List<Msg> inputMessages = preReasoningEvent.getInputMessages();
                Map<String, Object> forwardedProps = new HashMap<>();
                Msg systemMsg = AIMsgUtils.getSystemMsg(inputMessages);
                Msg lastUserMsg = AIMsgUtils.getLastUserMsg(inputMessages);
                if (Objects.nonNull(lastUserMsg)) {
                    forwardedProps.putAll(
                            Optional.ofNullable((Map<String, Object>) lastUserMsg.getMetadata().get(FORWARDED_PROPS_KEY))
                                    .orElse(Collections.emptyMap())
                    );
                }


                if (Objects.nonNull(systemMsg) && !forwardedProps.isEmpty()) {
                    List<ContentBlock> blocks = systemMsg.getContent()
                            .stream()
                            .map(v -> {
                                if (v instanceof TextBlock textBlock) {
                                    String systemPrompt = templateRenderer.apply(textBlock.getText(), forwardedProps);
                                    return TextBlock.builder().text(systemPrompt).build();
                                }
                                return v;
                            })
                            .toList();


                    Msg newSysPrompt = Msg.builder()
                            .id(systemMsg.getId())
                            .role(systemMsg.getRole())
                            .metadata(systemMsg.getMetadata())
                            .content(blocks)
                            .timestamp(systemMsg.getTimestamp())
                            .build();
                    AIMsgUtils.replaceSystemMsg(inputMessages, newSysPrompt);
                }

            }

            return Mono.just(event);
        }
    }

}
