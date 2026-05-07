package org.quyq.gwsu.security.brain.service.tool;


import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolEmitter;
import io.agentscope.core.tool.ToolParam;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;

import java.util.Map;

/**
 * Web端工具集合
 * 通过SpringUtils获取WebToolUtils Bean，因为WebTool由AgentScope反射实例化，非Spring管理
 */
public class WebTool {

    //@HumanInTheLoop(tip = "是否同意路由跳转？")
    @Tool(description = "控制web界面跳转到指定路由")
    public ToolResultBlock routeNavigation(@ToolParam(name = "path", description = "跳转的路由地址") String path,
                                           ToolEmitter emitter) {
        return SpringUtils.getBean(WebToolUtils.class)
                .webExecuteTool(emitter, "routeNavigation", Map.of("path", path));
    }
}
