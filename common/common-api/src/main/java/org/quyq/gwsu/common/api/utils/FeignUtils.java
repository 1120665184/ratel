package org.quyq.gwsu.common.api.utils;


import org.quyq.gwsu.common.api.exception.FeignException;
import org.quyq.gwsu.common.core.domain.R;

import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/4/20
 * @description
 */
public class FeignUtils {

    private FeignUtils() {
    }

    /**
     * 获取微服务接口返回数据对象
     *
     * @param r
     * @param <T>
     * @return
     */
    public static <T> T data(R<T> r) {
        if (Objects.isNull(r)) {
            throw new NullPointerException("r is null");
        }
        if (r.isSuccess()) {
            return r.data();
        }
        throw new FeignException(r.errCode(), r.msg());
    }

}
