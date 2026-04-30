package org.quyq.gwsu.common.core.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.exception.BasicException;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.springframework.http.HttpStatus;

/**
 * @author Quyq
 * @date 2026/3/10
 * @description 接口统一返回类型
 */
public record R<T>(
        int code,
        @NonNull
        String msg,
        T data,
        @Nullable
        String errCode
) {

    @JsonIgnore
    public boolean isSuccess() {
        return CoreConstants.Code.SUCCESS.getCode() == code;
    }

    public static <T> R<T> ok(T data) {
        return new R<>(CoreConstants.Code.SUCCESS.getCode(), CoreConstants.Code.SUCCESS.getMsg(), data, null);
    }

    public static <T> R<T> ok(T data, String msg) {
        return new R<>(CoreConstants.Code.SUCCESS.getCode(), msg, data, null);
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail() {
        return new R<>(CoreConstants.Code.ERROR.getCode(), CoreConstants.Code.ERROR.getMsg(), null, null);
    }

    public static <T> R<T> fail(String msg) {
        return new R<>(CoreConstants.Code.ERROR.getCode(), msg, null, null);
    }

    public static <T> R<T> fail(BasicException exception) {
        int c = CoreConstants.Code.ERROR.getCode();
        if (CommonErrorCode.E03001 == exception.getCode()) {
            c = HttpStatus.UNAUTHORIZED.value();
        }
        return new R<>(c, exception.getErrMsg(), null, exception.generateErrorCode());
    }

}
