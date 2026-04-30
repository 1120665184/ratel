package org.quyq.gwsu.gateway.exception;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.exception.ExceptionMsgHandler;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webflux.autoconfigure.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import reactor.core.publisher.Mono;

/**
 * 全局异常处理
 */
@Slf4j
public class GatewayErrorWebExceptionHandler extends DefaultErrorWebExceptionHandler {

    private final Gson gson = new Gson();

    public GatewayErrorWebExceptionHandler(
            ErrorAttributes errorAttributes,
            WebProperties.Resources resources,
            ErrorProperties errorProperties,
            ApplicationContext applicationContext) {
        super(errorAttributes, resources, errorProperties, applicationContext);
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    @Override
    protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);

        HttpStatusCode code;
        R<Void> result;
        if (error instanceof NotFoundException || error instanceof NoResourceFoundException) {
            code = HttpStatus.NOT_FOUND;
            result = R.fail(CommonErrorCode.E00001.msg());
        } else {
            ExceptionMsgHandler.ErrorInfo errorInfo = ExceptionMsgHandler.determineErrorInfo(error);
            code = errorInfo.code();
            result = errorInfo.result();
        }


        return ServerResponse
                .status(code)
                .contentType(MediaType.APPLICATION_JSON)
                .body(writeResponseAsMono(result), String.class);
    }

    @Override
    protected void logError(ServerRequest request, ServerResponse response, Throwable throwable) {
        //ignore
    }

    private Mono<String> writeResponseAsMono(R<Void> result) {
        return Mono.just(gson.toJson(result));
    }
}
