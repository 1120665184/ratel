package org.quyq.gwsu.common.core.exception.handler;


import org.quyq.gwsu.common.core.exception.ExceptionMsgHandler;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author Quyq
 * @date 2026/4/8
 * @description
 */
public class GlobalExceptionFunctionHandler implements HandlerFilterFunction<ServerResponse, ServerResponse> {
    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        try {
            return next.handle(request);
        } catch (Exception e) {
            ExceptionMsgHandler.ErrorInfo errorInfo = ExceptionMsgHandler.determineErrorInfo(e);
            return ServerResponse
                    .status(errorInfo.code())
                    .body(errorInfo.result());
        }
    }
}
