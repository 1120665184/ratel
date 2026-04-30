package org.quyq.gwsu.common.api.exception;


import org.quyq.gwsu.common.core.exception.BasicException;

/**
 * @author Quyq
 * @date 2026/4/20
 * @description
 */
public class FeignException extends BasicException {

    private final String errCodeMsg;

    private final String errMsg;

    public FeignException(String errCode, String message) {
        super(message);
        this.errCodeMsg = errCode;
        this.errMsg = message;
    }

    @Override
    protected String errorType() {
        return "";
    }

    @Override
    public String getErrMsg() {
        return errMsg;
    }

    @Override
    public String generateErrorCode() {
        return errCodeMsg;
    }
}
