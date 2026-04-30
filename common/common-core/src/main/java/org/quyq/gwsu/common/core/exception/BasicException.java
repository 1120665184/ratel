package org.quyq.gwsu.common.core.exception;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.exception.errcode.ErrorCodeMeta;

import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/3/23
 * @description
 */
@Slf4j
public abstract class BasicException extends RuntimeException {

    /**
     * 错误码
     */
    @Getter
    private final transient ReturnCode code;

    @Getter
    private final String errMsg;


    /**
     * 错误类型，最终错误码前缀，可以通过前缀确认错误的大致分类
     *
     * @return
     */
    protected abstract String errorType();


    public BasicException(ReturnCode code) {
        super(code.msg());
        this.code = code;
        this.errMsg = code.msg();
    }

    public BasicException(ReturnCode code, String message) {
        super(message);
        this.code = code;
        this.errMsg = message;
    }

    public BasicException(ReturnCode code, String message, Throwable cause) {
        super(cause);
        this.code = code;
        this.errMsg = message;
    }

    public BasicException(ReturnCode code, Throwable cause) {
        super(cause);
        this.code = code;
        this.errMsg = code.msg();
    }

    public BasicException(String message) {
        super(message);
        this.code = CommonErrorCode.E00000;
        this.errMsg = this.code.msg();
    }

    public BasicException(Throwable cause) {
        super(cause);
        this.code = CommonErrorCode.E00000;
        this.errMsg = this.code.msg();
    }


    /**
     * 错误码生成
     *
     * @return
     */
    public String generateErrorCode() {
        ErrorCodeMeta meta = this.code.getClass().getAnnotation(ErrorCodeMeta.class);
        if (Objects.isNull(meta)) {
            log.error("{} 未标识ErrorCodeMeta注解配置，生成错误码失败", this.code.getClass());
            return null;
        }

        return "%s_%s_%s".formatted(errorType(), meta.moduleCode(), code.code());
    }


}
