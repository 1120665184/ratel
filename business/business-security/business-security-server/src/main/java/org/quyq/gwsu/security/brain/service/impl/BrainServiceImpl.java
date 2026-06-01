package org.quyq.gwsu.security.brain.service.impl;


import io.agentscope.core.agent.Agent;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.processor.AguiRequestProcessor;
import io.agentscope.core.agui.registry.AguiAgentRegistry;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.session.Session;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.agui.DefaultAgentResolver;
import org.quyq.gwsu.common.ai.agui.ThreadSessionManager;
import org.quyq.gwsu.common.ai.agui.tool.AskUserQuestionTool;
import org.quyq.gwsu.common.ai.agui.web.WebToolExecuteHook;
import org.quyq.gwsu.common.ai.utils.AIMsgUtils;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.api.menu.vo.MenuVO;
import org.quyq.gwsu.security.brain.ModelProvider;
import org.quyq.gwsu.security.brain.service.IBrainService;
import org.quyq.gwsu.security.brain.service.agent.DatabaseSearchAgent;
import org.quyq.gwsu.security.brain.service.agent.OutputViewAgent;
import org.quyq.gwsu.security.brain.service.hook.OutputViewEventHandlerHook;
import org.quyq.gwsu.security.brain.service.tool.WebTool;
import org.quyq.gwsu.security.menu.service.ISecurityMenuService;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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

    private final Session agentSession;

    private final SecurityUtils securityUtils;


    private final ISecurityMenuService menuService;

    private final WebTool webTool;

    private final ObjectMapper objectMapper;


    public Agent buildAgent() {
        Toolkit toolkit = toolkitProvider.getIfAvailable(Toolkit::new);

        toolkit.registerTool(new AskUserQuestionTool());

        // 构建包含当前用户菜单权限信息的技能
        SkillBox skillBox = buildSkillBox(toolkit);

        return getAgent(toolkit, skillBox);
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
                .description("当需要操作用户可视化界面时，加载此技能查看用户的完整菜单和功能列表以及获取操作界面相关工具")
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
        if (menuTree == null || menuTree.isEmpty()) {
            return "当前用户没有任何菜单权限。";
        }

        String menuSections = menuTree.stream()
                .map(menu -> buildMenuSection(menu, 2))
                .collect(Collectors.joining("\n"));

        return """
                # 用户功能权限信息
                
                以下是当前登录用户拥有的所有菜单、页面和操作按钮信息。
                当用户请求跳转界面或执行操作时，请参考此列表判断用户是否有对应功能，并使用路由地址进行导航。
                
                ---
                
                %s
                
                ---
                
                # 备注
                - **路由**：前端视图层界面跳转地址
                - **位置**：菜单在视图层的展示位置
                - **接口权限**：菜单或按钮对应的后端接口权限标识，`(main)` 标注的为对应功能的主要接口，多个权限用 `;` 分割
                - **按钮标识**：对应按钮的唯一标识，用于判定视图层按钮的显示权限
                """.formatted(menuSections);
    }

    /**
     * 构建单个菜单/目录节点的 markdown 段落
     */
    private String buildMenuSection(MenuVO menu, int headingLevel) {
        String heading = "#".repeat(headingLevel);
        String typeLabel = switch (menu.getMenuType()) {
            case 1 -> "目录";
            case 2 -> "菜单";
            case 3 -> "按钮";
            default -> "未知";
        };

        StringBuilder sb = new StringBuilder();

        // 标题行：名称 [类型]
        sb.append(heading).append(" ").append(menu.getMenuName())
                .append(" `").append(typeLabel).append("`\n\n");

        // 基本信息（非按钮类型）
        if (menu.getMenuType() != 3) {
            sb.append("| 属性 | 值 |\n|------|----|\n");
            sb.append("| 类型 | ").append(typeLabel).append(" |\n");
            if (2 == menu.getMenuType() && menu.getPath() != null) {
                sb.append("| 路由 | `").append(menu.getPath()).append("` |\n");
            }
            if (Arrays.asList(1, 2).contains(menu.getMenuType()) && menu.getPosition() != null) {
                sb.append("| 位置 | ").append(menu.getPosition().getDescription()).append(" |\n");
            }
            if (StringUtils.hasText(menu.getPermission())) {
                sb.append("| 接口权限 | `").append(menu.getPermission()).append("` |\n");
            }
            sb.append("\n");
        }

        // 功能描述
        if (StringUtils.hasText(menu.getDescription())) {
            sb.append(formatDescription(menu.getDescription())).append("\n\n");
        }

        // 按钮操作表格
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            List<MenuVO> buttons = menu.getChildren().stream()
                    .filter(child -> child.getMenuType() != null && child.getMenuType() == 3)
                    .toList();

            if (!buttons.isEmpty()) {
                sb.append("**操作按钮：**\n\n");
                sb.append("| 按钮 | 标识 | 接口权限 | 说明 |\n");
                sb.append("|------|------|----------|------|\n");
                for (MenuVO btn : buttons) {
                    String desc = StringUtils.hasText(btn.getDescription()) ? btn.getDescription().replace("|", "\\|").replace("\n", " ") : "-";
                    String perm = StringUtils.hasText(btn.getPermission()) ? "`" + btn.getPermission() + "`" : "-";
                    String key = StringUtils.hasText(btn.getButtonKey()) ? "`" + btn.getButtonKey() + "`" : "-";
                    sb.append("| ").append(btn.getMenuName()).append(" | ")
                            .append(key).append(" | ")
                            .append(perm).append(" | ")
                            .append(desc).append(" |\n");
                }
                sb.append("\n");
            }

            // 子目录/子菜单递归
            List<MenuVO> subMenus = menu.getChildren().stream()
                    .filter(child -> child.getMenuType() == null || child.getMenuType() != 3)
                    .toList();

            for (MenuVO child : subMenus) {
                sb.append(buildMenuSection(child, headingLevel + 1));
            }
        }

        return sb.toString();
    }

    /**
     * 格式化描述内容，当描述包含 markdown 语法时使用折叠块隔离，避免影响外层文档可读性
     */
    private String formatDescription(String description) {
        boolean hasMarkdownSyntax = description.contains("#") || description.contains("```")
                || description.contains("- ") || description.contains("* ") || description.contains("| ")
                || description.contains("> ") || description.contains("1. ");

        if (hasMarkdownSyntax) {
            return """
                    <details>
                    <summary>📋 功能说明</summary>
                    
                    %s
                    
                    </details>""".formatted(description);
        }

        return "> " + description.lines().map(l -> "> " + l).collect(Collectors.joining("\n"));
    }


    private Agent getAgent(Toolkit toolkit, SkillBox skillBox) {
        //内容输出子智能体
        toolkit.registration()

                .subAgent(outputViewAgent::build, outputViewAgent.getSubAgentConfig())
                .apply();
        //数据库智能查询智能体
        toolkit.registration()
                //数据库搜索子智能体
                .subAgent(databaseSearchAgent::build, databaseSearchAgent.getSubAgentConfig())
                .apply();

        OutputViewEventHandlerHook outputViewEventHandlerHook = new OutputViewEventHandlerHook(objectMapper);
        WebToolExecuteHook webToolExecuteHook = new WebToolExecuteHook();

        return HarnessAgent.builder()
                .name("CentralBrain")
                .session(agentSession)
                .sysPrompt(buildSysPrompt())
                .model(ModelProvider.generateModel())
                .hook(new ForwardedPropsHook())
                .hook(outputViewEventHandlerHook)
                .hook(webToolExecuteHook)
                .toolkit(toolkit)
                .skillBox(skillBox)
                .maxIters(100)
                .toolExecutionConfig(ExecutionConfig.builder()
                        .timeout(Duration.of(10, ChronoUnit.MINUTES))
                        .build())
                .enableAgentTracingLog(true)
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
                - 用户询问功能权限、想跳转页面、想执行操作时，加载 user_menu_permissions 技能查看其拥有的功能
                - 根据技能中的路由地址和功能描述，决定使用 RouteNavigation 工具导航到对应页面
                
                # AI输出面板
                你拥有专属的AI输出面板，可以给用户输出可视化形式的内容（如图表、表格等）。当你输出内容时，优先考虑是否应该使用该面板以更直观的方式展示信息。
                
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
