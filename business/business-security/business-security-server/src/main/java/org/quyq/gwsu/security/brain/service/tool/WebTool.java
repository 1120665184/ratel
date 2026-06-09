package org.quyq.gwsu.security.brain.service.tool;


import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolEmitter;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.domain.AIRunnerInstanceWrapper;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.ai.loop.ApprovalCondition;
import org.quyq.gwsu.common.ai.loop.HumanInTheLoop;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Web端工具集合
 * 提供AI助手查看和操作Web界面的能力
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class WebTool {

    private final WebToolUtils webToolUtils;


    // ==================== 操作模式 ====================

    @HumanInTheLoop(tip = "智能助手请求控制界面，是否同意？")
    @Tool(name = "EnterAiMode", description = """
            请求进入`AI操作模式`，获取界面控制权。
            调用此工具后，界面将锁定为AI操作模式，用户无法手动操作界面。
            使用场景：当你需要对界面进行操作（点击、输入、选择、滚动、路由跳转）时，必须先调用此工具获取控制权。
            注意：获取页面状态(GetPageState)不需要进入AI操作模式。
            操作完成后必须调用ExitAiMode退出AI操作模式，将控制权交还给用户。""")
    public Mono<ToolResultBlock> enterAiMode() {
        return Mono.deferContextual(ctx -> {
            AIRunnerInstanceWrapper wrapper = ctx.get(AIConstants.Param.EMITTER_WRAPPER);
            return Mono.just(webToolUtils.webExecuteTool(wrapper, "EnterAiMode", Map.of()));
        });
    }

    @Tool(name = "ExitAiMode", description = """
            退出`AI操作模式`，将界面控制权交还给用户。
            当你完成所有界面操作后，必须调用此工具退出AI操作模式。""")
    public Mono<ToolResultBlock> exitAiMode() {
        return Mono.deferContextual(ctx -> {
            AIRunnerInstanceWrapper wrapper = ctx.get(AIConstants.Param.EMITTER_WRAPPER);
            return Mono.just(webToolUtils.webExecuteTool(wrapper, "ExitAiMode", Map.of()));
        });

    }

    // ==================== 路由导航 ====================

    @Tool(name = "RouteNavigation", description = """
            控制web界面跳转到指定路由
            工具使用前提：界面操作模式必须是`AI操作模式`。
            """)
    public Mono<ToolResultBlock> routeNavigation(@ToolParam(name = "path",
            description = """
                    跳转的前端路由地址
                    示例：/sub-system/user
                    """) String path) {

        return Mono.deferContextual(ctx -> {
            AIRunnerInstanceWrapper wrapper = ctx.get(AIConstants.Param.EMITTER_WRAPPER);
            return Mono.just(webToolUtils
                    .webExecuteTool(wrapper, "RouteNavigation", Map.of("path", path)));
        });

    }


    // ==================== 查看界面 ====================

    @Tool(name = "GetPageState", description = """
            获取当前Web界面的状态信息，返回页面中可见的交互元素列表和页面基本信息。
            返回内容为简化的HTML文本，每个可交互元素带有索引编号和标签信息。
            
            元素格式说明：
            - [index]{tags}元素： []标识的内容为元素索引编号，用该编号定位操作元素,必有 ，{}包裹的为元素标签，用户对元素的额外功能标注，多个,分割，由前端生成，可能不包含
              示例：  普通元素：[0]<button>提交</button>  带标签的元素：[0]{approval}<button>提交</button>
            """)
    public Mono<ToolResultBlock> getPageState() {
        return Mono.deferContextual(ctx -> {
            AIRunnerInstanceWrapper wrapper = ctx.get(AIConstants.Param.EMITTER_WRAPPER);
            return Mono.just(webToolUtils.webExecuteTool(wrapper, "GetPageState", Map.of()));
        });
    }

    // ==================== 操作界面 ====================

    @HumanInTheLoop(tip = "该操作需要人工审批确认，是否继续？", reasoningCondition = NeedClickApprovalCondition.class)
    @Tool(name = "ClickElement", description = """
            通过元素索引点击界面上的元素。执行完整的W3C指针事件序列。
            需要先调用GetPageState获取元素索引。
            工具使用前提：界面操作模式必须是`AI操作模式`。
            
            参数：
            - index：元素索引编号
            - operationDescription：本次点击的简要描述：例如：查看用户列表，保存用户信息，删除用户
            - tags：元素的标签信息，从GetPageState结果中{}包裹的内容获取，没有tags时传空字符串即可""")
    public Mono<ToolResultBlock> clickElement(
            @ToolParam(name = "index", description = "要点击的元素索引编号，从GetPageState结果中获取") Integer index,
            @ToolParam(name = "operationDescription", description = "本次点击的简要描述：例如：查看用户列表，保存用户信息，删除用户") String operationDescription,
            @ToolParam(name = "tags", description = "元素的标签信息，从GetPageState结果中{}包裹的内容获取，没有tags时传空字符串即可", required = false) String tags) {

        return Mono.deferContextual(ctx -> {
            AIRunnerInstanceWrapper wrapper = ctx.get(AIConstants.Param.EMITTER_WRAPPER);
            return Mono.just(webToolUtils.webExecuteTool(wrapper, "ClickElement", Map.of("index", index)));
        });
    }

    @Tool(name = "InputText", description = """
            在指定索引的输入框中输入文本。会先清空现有内容再输入新文本，兼容React受控组件。
            需要先调用GetPageState获取输入框的元素索引。
            工具使用前提：界面操作模式必须是`AI操作模式`。
            参数：index - 输入框元素索引编号，text - 要输入的文本内容""")
    public Mono<ToolResultBlock> inputText(
            @ToolParam(name = "index", description = "输入框元素索引编号") Integer index,
            @ToolParam(name = "text", description = "要输入的文本内容") String text) {
        return Mono.deferContextual(ctx -> {
            AIRunnerInstanceWrapper wrapper = ctx.get(AIConstants.Param.EMITTER_WRAPPER);
            return Mono.just(webToolUtils.webExecuteTool(wrapper, "InputText", Map.of("index", index, "text", text)));
        });
    }

    @Tool(name = "SelectOption", description = """
            在指定索引的下拉选择框中选择选项。支持原生select元素和Ant Design Select组件。
            需要先调用GetPageState获取select元素的索引。
            工具使用前提：界面操作模式必须是`AI操作模式`。
            参数：index - select元素索引编号，text - 要选择的选项文本""")
    public Mono<ToolResultBlock> selectOption(
            @ToolParam(name = "index", description = "select元素索引编号") Integer index,
            @ToolParam(name = "text", description = "要选择的选项文本") String text) {
        return Mono.deferContextual(ctx -> {
            AIRunnerInstanceWrapper wrapper = ctx.get(AIConstants.Param.EMITTER_WRAPPER);
            return Mono.just(webToolUtils.webExecuteTool(wrapper, "SelectOption", Map.of("index", index, "text", text)));
        });
    }

    @Tool(name = "HoverElement", description = """
            将鼠标悬停在指定索引的元素上，触发hover效果使隐藏的交互元素显示出来。
            使用场景：某些按钮或操作入口仅在鼠标悬停时才显示，例如：
            - 表格行悬停后出现的编辑/删除按钮
            - 下拉选择框悬停后出现的清除/关闭按钮
            - 卡片悬停后出现的操作按钮
            需要先调用GetPageState获取元素索引。
            工具使用前提：界面操作模式必须是`AI操作模式`。
            悬停后请调用GetPageState查看新出现的交互元素。
            参数：index - 要悬停的元素索引编号""")
    public Mono<ToolResultBlock> hoverElement(
            @ToolParam(name = "index", description = "要悬停的元素索引编号，从GetPageState结果中获取") Integer index,
            ToolEmitter emitter) {
        return Mono.deferContextual(ctx -> {
            AIRunnerInstanceWrapper wrapper = ctx.get(AIConstants.Param.EMITTER_WRAPPER);
            return Mono.just(webToolUtils.webExecuteTool(wrapper, "HoverElement", Map.of("index", index)));
        });
    }

    @Tool(name = "ScrollPage", description = """
            滚动页面或页面内的可滚动元素。
            工具使用前提：界面操作模式必须是`AI操作模式`。
            参数：direction - 滚动方向(up/down/left/right)，amount - 滚动量(1=一页，像素值=精确滚动)""")
    public Mono<ToolResultBlock> scrollPage(
            @ToolParam(name = "direction", description = "滚动方向：up/down/left/right") String direction,
            @ToolParam(name = "amount", description = "滚动量：1表示一页，数字像素值表示精确滚动") Integer amount) {
        return Mono.deferContextual(ctx -> {
            AIRunnerInstanceWrapper wrapper = ctx.get(AIConstants.Param.EMITTER_WRAPPER);
            return Mono.just(webToolUtils.webExecuteTool(wrapper, "ScrollPage",
                    Map.of("direction", direction, "amount", amount)));
        });
    }


    public static class NeedClickApprovalCondition implements ApprovalCondition {
        private static final String APPROVAL_TAG = "approval";

        @Override
        public Outcome invoke(Map<String, Object> args) {
            Object tags = args.get("tags");
            String operationDescription = (String) args.get("operationDescription");
            String tipStart = "敏感操作审批：";
            String tip = null;
            boolean needApproval = false;
            if (Objects.nonNull(tags)) {
                if (StringUtils.hasText(operationDescription)) {
                    tip = tipStart + operationDescription;
                }

                if (tags instanceof String tagsStr && StringUtils.hasText(tagsStr)) {
                    needApproval = tagsStr.contains(APPROVAL_TAG);
                }
                // tags 为 List 类型时（某些LLM可能传数组）
                else if (tags instanceof List<?> tagsList) {
                    needApproval = tagsList.stream().anyMatch(t -> APPROVAL_TAG.equals(String.valueOf(t)));
                }
            }


            return Outcome.builder()
                    .needApproval(needApproval)
                    .tip(tip)
                    .build();
        }

    }

}
