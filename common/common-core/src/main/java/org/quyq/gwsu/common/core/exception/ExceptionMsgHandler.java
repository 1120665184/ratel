package org.quyq.gwsu.common.core.exception;


import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Quyq
 * @date 2026/4/5
 * @description
 */
@Slf4j
public class ExceptionMsgHandler {

    private ExceptionMsgHandler() {
    }

    /**
     * 通过异常生成最终错误信息
     *
     * @param ex
     * @return
     */
    public static ErrorInfo determineErrorInfo(Throwable ex) {

        HttpStatusCode status = determineHttpStatus(ex);
        R<Void> result = R.fail(CommonErrorCode.E00000.msg());
        if (ex instanceof BasicException basic) {
            result = basicR(basic);
        } else if (ex.getCause() instanceof BasicException basic) {
            result = basicR(basic);
        } else {
            log.error("", ex);
        }


        return new ErrorInfo(status, result);
    }

    private static R<Void> basicR(BasicException basic) {
        if (CommonErrorCode.E00000 == basic.getCode()) {
            log.error("", basic);
        }

        return R.fail(basic);
    }


    private static HttpStatusCode determineHttpStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode();
        } else if (ex instanceof BasicException bex) {
            ReturnCode code = bex.getCode();
            //登录已失效，返回响应码401
            if (code == CommonErrorCode.E03001) {
                return HttpStatus.UNAUTHORIZED;
            }
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }


    public record ErrorInfo(
            /**
             * http响应码
             */
            HttpStatusCode code,
            /**
             * 响应内容
             */
            R<Void> result
    ) {

    }


}
