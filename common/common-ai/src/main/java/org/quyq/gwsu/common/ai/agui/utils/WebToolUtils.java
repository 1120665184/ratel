package org.quyq.gwsu.common.ai.agui.utils;


import com.google.gson.Gson;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolEmitter;
import org.quyq.gwsu.common.ai.agui.web.WebToolExecuteHook;

import java.util.Map;

/**
 * @author Quyq
 * @date 2026/5/6
 * @description
 */
public class WebToolUtils {

    private WebToolUtils() {
    }

    public static final String WEB_TOOL_IDENTIFICATION = "NOTICE_WEB_TOOL:";

    /**
     * 通知web端执行指定工具
     *
     * @param toolEmitter
     * @param toolName
     * @param params
     */
    public static void webExecuteTool(ToolEmitter toolEmitter, String toolName, Map<String, Object> params) {
        WebToolExecuteHook.WebToolInfo info = new WebToolExecuteHook.WebToolInfo(toolName, params);
        Gson gson = new Gson();
        toolEmitter.emit(
                ToolResultBlock.text(WEB_TOOL_IDENTIFICATION + gson.toJson(info))
        );

    }

}
