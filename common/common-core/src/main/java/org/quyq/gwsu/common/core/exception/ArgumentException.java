package org.quyq.gwsu.common.core.exception;


import org.quyq.gwsu.common.core.domain.ReturnCode;

/**
 * @author Quyq
 * @date 2026/4/13
 * @description
 */
public class ArgumentException extends BasicException {

    public ArgumentException(ReturnCode code) {
        super(code);
    }

    public ArgumentException(ReturnCode code, String message) {
        super(code, message);
    }

    public ArgumentException(ReturnCode code, Throwable cause) {
        super(code, cause);
    }

    public ArgumentException(String message) {
        super(message);
    }

    public ArgumentException(Throwable cause) {
        super(cause);
    }

    @Override
    protected String errorType() {
        return "ARGS";
    }
}
