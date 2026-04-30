package org.quyq.gwsu.common.authentication.exception;


import lombok.Getter;
import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.BasicException;

/**
 * @author Quyq
 * @date 2026/4/8
 * @description
 */
@Getter
public class AuthException extends BasicException {


    private String username;

    public AuthException(String username, ReturnCode code) {
        super(code);
        this.username = username;
    }

    public AuthException(ReturnCode code) {
        super(code);
    }

    public AuthException(String username ,ReturnCode code, String message) {
        super(code, message);
        this.username = username;
    }

    public AuthException(ReturnCode code, String message) {
        super(code, message);
    }

    public AuthException(ReturnCode code, Throwable cause) {
        super(code, cause);
    }

    public AuthException(String message) {
        super(message);
    }

    public AuthException(Throwable cause) {
        super(cause);
    }

    @Override
    protected String errorType() {
        return "AUTH";
    }
}
