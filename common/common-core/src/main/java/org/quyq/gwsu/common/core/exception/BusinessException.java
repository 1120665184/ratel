package org.quyq.gwsu.common.core.exception;


import org.quyq.gwsu.common.core.domain.ReturnCode;

/**
 * @author Quyq
 * @date 2026/3/23
 * @description 业务异常
 */
public class BusinessException extends BasicException {


    public BusinessException(ReturnCode code) {
        super(code);
    }

    public BusinessException(ReturnCode code, String message) {
        super(code, message);
    }

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(Throwable cause) {
        super(cause);
    }

    @Override
    protected String errorType() {
        return "BUSI";
    }
}
