package org.quyq.gwsu.common.core.exception.handler;


import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.exception.ExceptionMsgHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author Quyq
 * @date 2026/4/5
 * @description 普通微服务全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<R<Void>> throwableHandler(Throwable throwable) {
        ExceptionMsgHandler.ErrorInfo errorInfo = ExceptionMsgHandler.determineErrorInfo(throwable);
        return ResponseEntity
                .status(errorInfo.code())
                .body(errorInfo.result());

    }

}
