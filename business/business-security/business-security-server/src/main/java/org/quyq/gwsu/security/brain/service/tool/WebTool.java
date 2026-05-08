package org.quyq.gwsu.security.brain.service.tool;


import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolEmitter;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.loop.HumanInTheLoop;
import org.quyq.gwsu.common.core.utils.SpringUtils;

import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Web端工具集合
 */
@RequiredArgsConstructor
public class WebTool {

    private final WebToolUtils webToolUtils;

    @HumanInTheLoop(tip = "是否同意路由跳转？")
    @Tool(name = "RouteNavigation", description = "控制web界面跳转到指定路由")
    public ToolResultBlock routeNavigation(@ToolParam(name = "path",
                                                   description = """
                                                           跳转的前端路由地址
                                                           示例：/sub-system/user
                                                           """) String path,
                                           ToolEmitter emitter) throws TimeoutException {

        return webToolUtils
                .webExecuteTool(emitter, "RouteNavigation", Map.of("path", path));
    }
}
