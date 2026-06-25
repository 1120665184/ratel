package org.quyq.gwsu.security.brain.service.impl;


import io.agentscope.core.agent.Agent;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.processor.AguiRequestProcessor;
import io.agentscope.core.agui.registry.AguiAgentRegistry;
import io.agentscope.core.hook.*;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.session.Session;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.DefaultAgentResolver;
import org.quyq.gwsu.common.ai.agui.ThreadSessionManager;
import org.quyq.gwsu.common.ai.agui.tool.AskUserQuestionTool;
import org.quyq.gwsu.common.ai.utils.AIMsgUtils;
import org.quyq.gwsu.common.core.domain.visitor.ClientInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.enums.VisitorType;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.SessionUtils;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private final SessionUtils sessionUtils;

    private final ISecurityMenuService menuService;

    private final WebTool webTool;

    private final ObjectMapper objectMapper;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");


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
                .name("system_view_operation")
                .description("当需要操作用户可视化界面时，加载此技能查看用户的完整菜单和功能列表以及获取操作界面相关工具")
                .skillContent(menuContent)
                .build();
        String loginType = sessionUtils.getLoginType();
        List<String> disableTool = new ArrayList<>();
        if ("headless".equals(loginType)) {
            disableTool = List.of("EnterAiMode", "ExitAiMode");
        }
        skillBox.registration()
                .skill(userMenuSkill)
                .tool(webTool)
                .disableTools(disableTool)
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
                # 系统操作安全助手 Skill
                
                ## 核心行为准则
                
                ### 准则1：永不编造
                - 路由地址必须使用下方菜单列表中的值，严禁自行编造或推测
                
                ### 准则2：缺失必问
                - 执行新增/编辑前，检查用户是否提供了所有必要字段
                - 如有缺失，逐一询问，不得跳过、不得猜测、不得使用默认值
                
                ### 准则3：操作必确认
                - 任何修改性操作（新增、编辑、删除）执行前，必须获得用户的明确确认
                
                ---
                
                ## 菜单与权限数据
                
                以下是当前登录用户拥有的所有菜单、页面和操作按钮信息。
                
                %s
                
                ---
                
                ## 字段说明
                
                | 字段 | 含义 |
                |------|------|
                | 路由 | 前端视图层界面跳转地址 |
                | 位置 | 菜单在视图层的展示位置 |
                | 接口权限 | 菜单或按钮对应的后端接口权限标识，`(main)`标注的为主要接口，多个权限用`;`分割 |
                | 按钮标识 | 按钮的唯一标识，用于判定视图层按钮的显示权限 |
                
                ---
                
                ## 操作流程
                
                ### 一、界面跳转
                1. 确认用户有对应菜单权限
                2. 使用列表中的**路由**地址进行跳转
                3. 禁止编造路由地址
                
                ### 二、新增/编辑操作
                
                **流程：**
                用户请求新增/编辑 → 检查字段完整性 → 如有缺失则逐一询问 → 收集完整后向用户复述 → 等待用户确认 → 执行保存
                
                **询问模板：**
                > “准备执行【操作名称】。目前还缺少以下信息：【字段1】、【字段2】，请补充。”
                
                **确认模板：**
                > “即将执行【操作名称】，信息汇总如下：
                > - 【字段A】：【值A】
                > - 【字段B】：【值B】
                >
                > 请确认无误后回复‘确认保存’。”
                
                **确认关键词：** `确认保存`、`确认`、`是`、`可以`、`保存`
                
                ### 三、删除操作
                
                **流程：**
                用户请求删除 → 确认删除对象 → 发出风险警告 → 等待用户输入确认关键词 → 执行删除
                
                
                **确认模板：**
                > “您确认要永久删除【对象名称/ID】吗？此操作不可撤销。请回复‘确认删除’以继续。”
                
                **确认关键词：** `确认删除`（必须完整匹配，不接受“是”、“好的”等模糊回复）
                
                ---
                
                ## 禁止行为清单
                
                - ❌ 在用户未提供完整字段时执行保存
                - ❌ 猜测或编造缺失字段的值
                - ❌ 跳过最终确认步骤直接操作
                - ❌ 对删除操作使用模糊确认词（如“是”“嗯”“好的”）
                - ❌ 编造不在菜单列表中的路由地址
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

        //构建当前登录主体的系统提示词
        String subjectSystemPrompt = buildSubjectSystemPrompt();

        return HarnessAgent.builder()
                .name("CentralBrain")
                .session(agentSession)
                .sysPrompt(buildSysPrompt())
                .model(ModelProvider.generateModel())
                .hooks(List.of(new StatisticsLLmCountHook(), new ForwardedPropsHook(subjectSystemPrompt), outputViewEventHandlerHook))
                .toolkit(toolkit)
                .skillBox(skillBox)
                .maxIters(100)
                .toolExecutionConfig(ExecutionConfig.builder()
                        .timeout(Duration.of(10, ChronoUnit.MINUTES))
                        .build())
                //   .enableAgentTracingLog(true)
                .disableSubagents()
                .disableShellTool()
                .disableMemoryTools()
                .disableFilesystemTools()
                .build();
    }

    /**
     * 、
     * 生成当前登录主体信息提示词
     *
     * @return
     */
    private String buildSubjectSystemPrompt() {
        Optional<Subject<Visitor>> subjectOpt = securityUtils.getSubject();
        if (subjectOpt.isEmpty()) {
            return null;
        }

        VisitorType visitorType = sessionUtils.getVisitorType();
        Subject<Visitor> subject = subjectOpt.get();
        String admin = subject.isAdmin() ? "是" : "否";
        String userType = VisitorType.USER == visitorType ? "平台用户" : "第三方客户端";
        String userInfo = "无";
        String clientInfo = "无";
        Optional<UserInfo> userInfoOpt = subject.userInfo();
        if (userInfoOpt.isPresent()) {
            userInfo = objectMapper.writeValueAsString(userInfoOpt.get());
        }
        Optional<ClientInfo> clientInfoOpt = subject.clientInfo();
        if (clientInfoOpt.isPresent()) {
            clientInfo = objectMapper.writeValueAsString(clientInfoOpt.get());
        }

        return """
                #当前系统时间
                %s
                # 当前登录主体信息：
                ## 主体类型：%s ,
                ## 是否超级管理员：%s ,
                ## 登录用户信息
                %s
                ## 所属三方平台信息
                %s
                """.formatted(dateTimeFormatter.format(LocalDateTime.now()), userType, admin, userInfo, clientInfo);
    }


    private String buildSysPrompt() {
        String loginType = sessionUtils.getLoginType();
        String headlessContent = "界面操作模式（human：人类操作模式 | ai：AI操作模式）：{operationMode}";
        if ("headless".equals(loginType)) {
            headlessContent = "**特别注意**：您当前已经处于“AI操作模式” ，可以直接调用操作界面相关工具 ，禁止调用`EnterAiMode`和`ExitAiMode`工具";
        }
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
                - 用户询问功能权限、想跳转页面、想执行操作时，加载 user_menu_permissions 技能查看其拥有的功能
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
                - %s
                
                """.formatted(headlessContent);
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
                .config(AguiAdapterConfig.builder()
                        .enableReasoning(true)
                        .build())
                .build();
    }

    @Slf4j
    private static class StatisticsLLmCountHook implements Hook {

        private final List<Statistics> LLMcount = new ArrayList<>();

        private final List<StatisticsTool> toolCount = new ArrayList<>();

        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {

            return Mono.defer(() -> {

                if (event instanceof PreReasoningEvent) {
                    LLMcount.add(new Statistics(LLMcount.size() + 1, LocalDateTime.now(), 0L));
                } else if (event instanceof PostReasoningEvent) {
                    Statistics last = LLMcount.getLast();
                    Duration between = Duration.between(last.start, LocalDateTime.now());
                    LLMcount.set(LLMcount.size() - 1, Statistics.build(last, between.toMillis()));

                }  else if (event instanceof PreActingEvent e){
                    ToolUseBlock toolUse = e.getToolUse();
                    toolCount.add(new StatisticsTool(toolCount.size() + 1, toolUse.getName() , LocalDateTime.now(), 0L ));

                } else if (event instanceof PostActingEvent e){
                    StatisticsTool last = toolCount.getLast();
                    Duration between = Duration.between(last.start, LocalDateTime.now());
                    toolCount.set(toolCount.size() - 1, StatisticsTool.build(last, between.toMillis()));
                }
                else if (event instanceof PostCallEvent) {
                    log.debug(printCountLog());
                    LLMcount.clear();
                    toolCount.clear();
                }


                return Mono.just(event);
            });
        }

        private String printCountLog() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            StringBuilder sb = new StringBuilder();

            // ========== 模型调用统计 ==========
            if (!LLMcount.isEmpty()) {
                sb.append("\n智能体运行完成，共调用 ").append(LLMcount.size()).append(" 次模型，每次开始时间和耗时如下：\n");
                sb.append("| 序号 | 开始时间                 | 耗时      |\n");
                sb.append("|------|--------------------------|-----------|\n");
                for (Statistics stat : LLMcount) {
                    sb.append("| ")
                            .append(String.format("%-4d", stat.count))
                            .append(" | ")
                            .append(stat.start.format(formatter))
                            .append(" | ")
                            .append(String.format("%-9s", formatSmartPerfect(stat.timeConsuming)))
                            .append(" |\n");
                }
            } else {
                sb.append("\n智能体运行完成，没有模型调用记录。");
            }

            // ========== 工具调用统计 ==========
            if (!toolCount.isEmpty()) {
                sb.append("\n工具调用统计（共 ").append(toolCount.size()).append(" 次）：\n");
                sb.append("| 序号 | 工具名称       | 开始时间                 | 耗时      |\n");
                sb.append("|------|----------------|--------------------------|-----------|\n");
                for (StatisticsTool stat : toolCount) {
                    sb.append("| ")
                            .append(String.format("%-4d", stat.count))
                            .append(" | ")
                            .append(String.format("%-14s", stat.toolName)) // 预留宽度，可自行调整
                            .append(" | ")
                            .append(stat.start.format(formatter))
                            .append(" | ")
                            .append(String.format("%-9s", formatSmartPerfect(stat.timeConsuming)))
                            .append(" |\n");
                }
            } else {
                // 如果已输出模型记录，但无工具调用，追加一行提示；若模型也无记录，前面已提示，这里避免重复
                if (!LLMcount.isEmpty()) {
                    sb.append("\n本次运行无工具调用。");
                }
            }

            return sb.toString();
        }

        public String formatSmartPerfect(long timeConsuming) {
            long millis = Math.abs(timeConsuming);

            if (millis == 0) {
                return "0秒";
            }
            if (millis < 1000) {
                return millis + "毫秒";
            }

            long seconds = millis / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            long remainSeconds = seconds % 60;
            long remainMinutes = minutes % 60;
            long remainHours = hours % 24;

            StringBuilder sb = new StringBuilder();
            if (days > 0) sb.append(days).append("天");
            if (remainHours > 0) sb.append(remainHours).append("小时");
            if (remainMinutes > 0) sb.append(remainMinutes).append("分");
            if (remainSeconds > 0) sb.append(remainSeconds).append("秒");

            // 兜底：如果上面什么都没加（比如刚好是整数小时），至少显示秒数
            if (sb.isEmpty()) {
                sb.append(remainSeconds).append("秒");
            }
            return sb.toString();
        }

        //统计工具调用
        record StatisticsTool(int count, String toolName, LocalDateTime start, long timeConsuming) {
            public static StatisticsTool build(StatisticsTool statisticsTool, long timeConsuming) {
                return new StatisticsTool(statisticsTool.count, statisticsTool.toolName, statisticsTool.start, timeConsuming);
            }
        }

        //统计模型调用次数
        record Statistics(int count, LocalDateTime start, long timeConsuming) {

            public static Statistics build(Statistics statistics, long timeConsuming) {
                return new Statistics(statistics.count, statistics.start, timeConsuming);
            }


        }


    }

    /**
     * 系统提示词动态参数替换
     */
    private static class ForwardedPropsHook implements Hook {

        public ForwardedPropsHook(String subjectSystemPrompt) {
            this.subjectSystemPrompt = subjectSystemPrompt;
        }

        private final StTemplateRenderer templateRenderer = StTemplateRenderer.builder().build();
        private final static String FORWARDED_PROPS_KEY = "forwardedProps";

        private final String subjectSystemPrompt;

        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {

            if (event instanceof PreReasoningEvent preReasoningEvent) {
                List<Msg> inputMessages = preReasoningEvent.getInputMessages();
                Map<String, Object> forwardedProps = new HashMap<>();
                Msg systemMsg = preReasoningEvent.getSystemMessage();
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
                            }).collect(Collectors.toList());
                    //添加登录主体的提示词
                    if (StringUtils.hasText(subjectSystemPrompt)) {
                        blocks.add(TextBlock.builder().text(subjectSystemPrompt).build());
                    }

                    Msg newSysPrompt = Msg.builder()
                            .id(systemMsg.getId())
                            .role(systemMsg.getRole())
                            .metadata(systemMsg.getMetadata())
                            .content(blocks)
                            .timestamp(systemMsg.getTimestamp())
                            .build();
                    preReasoningEvent.setSystemMessage(newSysPrompt);
                }

            }

            return Mono.just(event);
        }
    }

}
