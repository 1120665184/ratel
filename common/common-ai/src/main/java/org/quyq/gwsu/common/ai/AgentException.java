package org.quyq.gwsu.common.ai;


import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.BasicException;

/**
 * @author Quyq
 * @date 2026/4/22
 * @description
 */
public class AgentException extends BasicException {
    public AgentException(ReturnCode code) {
        super(code);
    }

    public AgentException(ReturnCode code, String message) {
        super(code, message);
    }

    public AgentException(ReturnCode code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public AgentException(ReturnCode code, Throwable cause) {
        super(code, cause);
    }

    public AgentException(String message) {
        super(message);
    }

    public AgentException(Throwable cause) {
        super(cause);
    }

    @Override
    protected String errorType() {
        return "AGENT";
    }
}
