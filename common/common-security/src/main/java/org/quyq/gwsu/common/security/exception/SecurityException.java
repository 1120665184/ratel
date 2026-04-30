package org.quyq.gwsu.common.security.exception;


import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.BasicException;

/**
 * @author Quyq
 * @date 2026/4/5
 * @description
 */
public class SecurityException extends BasicException {


    public SecurityException(ReturnCode code) {
        super(code);
    }

    public SecurityException(ReturnCode code, String message) {
        super(code, message);
    }

    public SecurityException(ReturnCode code, Throwable cause) {
        super(code, cause);
    }

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(Throwable cause) {
        super(cause);
    }

    @Override
    protected String errorType() {
        return "SECURITY";
    }
}
