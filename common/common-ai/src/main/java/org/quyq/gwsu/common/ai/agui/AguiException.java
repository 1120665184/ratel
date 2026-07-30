package org.quyq.gwsu.common.ai.agui;

import org.quyq.gwsu.common.core.exception.BasicException;

/**
 * AG-UI 协议层基础异常。
 *
 * @author Quyq
 * @date 2026/7/27
 */
public class AguiException extends BasicException {

    public AguiException(String message) {
        super(message);
    }

    public AguiException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    @Override
    protected String errorType() {
        return "AGUI";
    }

    public static class AgentNotFoundException extends AguiException {

        public AgentNotFoundException(String agentId) {
            super("Agent not found: " + agentId);
        }
    }

    public static class EncodingException extends AguiException {

        public EncodingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
