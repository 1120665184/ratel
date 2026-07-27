package org.quyq.gwsu.security.brain.service.tool.web;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.quyq.gwsu.common.ai.agui.model.AIRunnerInstanceWrapper;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 点击页面元素工具。
 */
@Component
public class ClickElementTool extends ToolBase {

    private static final String TOOL_NAME = "ClickElement";
    private static final String DEFAULT_APPROVAL_TIP = "该操作需要人工审批确认，是否继续？";
    private static final String APPROVAL_TAG = "approval";

    private final WebToolUtils webToolUtils;

    public ClickElementTool(WebToolUtils webToolUtils) {
        super(ToolBase.builder()
                .name(TOOL_NAME)
                .description("""
                        通过元素索引点击界面上的元素。执行完整的W3C指针事件序列。
                        需要先调用GetPageState获取元素索引。
                        工具使用前提：界面操作模式必须是`AI操作模式`。
                        
                        参数：
                        - index：元素索引编号
                        - operationDescription：本次点击的简要描述：例如：查看用户列表，保存用户信息，删除用户
                        - tags：元素的标签信息，从GetPageState结果中{}包裹的内容获取，没有tags时传空字符串即可""")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "index", Map.of(
                                        "type", "integer",
                                        "description", "要点击的元素索引编号，从GetPageState结果中获取"
                                ),
                                "operationDescription", Map.of(
                                        "type", "string",
                                        "description", "本次点击的简要描述：例如：查看用户列表，保存用户信息，删除用户"
                                ),
                                "tags", Map.of(
                                        "type", "string",
                                        "description", "元素的标签信息，从GetPageState结果中{}包裹的内容获取，没有tags时传空字符串即可"
                                )
                        ),
                        "required", List.of("index", "operationDescription")
                )));
        this.webToolUtils = webToolUtils;
    }

    @Override
    public Mono<PermissionDecision> checkPermissions(Map<String, Object> input, PermissionContextState contextState) {
        return Mono.just(needApproval(input) ? PermissionDecision.ask(buildApprovalTip(input)) : PermissionDecision.passthrough(TOOL_NAME));
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        AIRunnerInstanceWrapper wrapper = param.getRuntimeContext().get(AIRunnerInstanceWrapper.class);
        Object index = param.getInput().get("index");
        return Mono.just(webToolUtils.webExecuteTool(wrapper, TOOL_NAME, Map.of("index", index)));
    }

    static boolean needApproval(Map<String, Object> input) {
        Object tags = input.get("tags");
        if (tags instanceof String tagsStr) {
            return StringUtils.hasText(tagsStr) && tagsStr.contains(APPROVAL_TAG);
        }
        if (tags instanceof List<?> tagsList) {
            return tagsList.stream().anyMatch(tag -> APPROVAL_TAG.equals(String.valueOf(tag)));
        }
        return false;
    }

    static String buildApprovalTip(Map<String, Object> input) {
        Object operationDescription = input.get("operationDescription");
        if (operationDescription instanceof String desc && StringUtils.hasText(desc)) {
            return "敏感操作审批：" + desc;
        }
        return DEFAULT_APPROVAL_TIP;
    }
}
