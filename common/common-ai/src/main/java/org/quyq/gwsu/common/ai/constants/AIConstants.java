package org.quyq.gwsu.common.ai.constants;


/**
 * @author Quyq
 * @date 2026/5/6
 * @description Ai模块公共常量
 */
public interface AIConstants {

    /**
     * 需要人工审批的工具名
     */
    String MSG_METADATA_APPROVAL_TOOLS_KEY = "approval_tools";

    interface AguiCustomEvent {

        /**
         * 人工审批事件
         */
        String HUMAN_APPROVAL = "HUMAN_APPROVAL";

        /**
         * web端工具执行事件
         */
        String TOOL_EXECUTE = "TOOL_EXECUTE";

    }

    interface Param {
        String THREAD_ID = "threadId";
        String EMITTER_WRAPPER = "servletHeaders";
        String FORWARDED_PROPS_KEY =  "forwardedProps";

        String FORWARDED_PROPS_OPERATION_MODE_KEY = "operationMode";
        String FORWARDED_PROPS_CURRENT_PATH_KEY = "currentPath";

    }

    interface ToolName {

        String ASK_USER_QUESTION = "AskUserQuestion";

    }

}
