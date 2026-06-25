package org.quyq.gwsu.security.headless.exception;


import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;

/**
 * @author Quyq
 * @date 2026/6/24
 * @description 会话正在运行异常
 */
public class SessionIsRunningException extends BusinessException {
    public SessionIsRunningException() {
        super(SecurityErrorCode.E07013);
    }
}
