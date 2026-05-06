package org.quyq.gwsu.security.brain.service.tool;


import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolEmitter;
import io.agentscope.core.tool.ToolParam;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;

import java.util.Map;

/**
 * @author Quyq
 * @date 2026/5/6
 * @description
 */
public class WebTool {


    @Tool(description = "控制web界面跳转到指定路由")
    public ToolResultBlock routeNavigation(@ToolParam(name = "path", description = "跳转的路由地址") String path,
                                           ToolEmitter emitter) {
        WebToolUtils.webExecuteTool(emitter, "routeNavigation", Map.of("path", path));
        return ToolResultBlock.text("跳转成功");

    }

}
