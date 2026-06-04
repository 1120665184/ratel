package org.quyq.gwsu.common.core.accessor;


import io.micrometer.context.ThreadLocalAccessor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.quyq.gwsu.common.core.utils.ServletUtils;

import java.util.Map;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/6/4
 * @description 请求头ThreadLocal自动注入react
 */
@Slf4j
public class HeadersContextThreadLocalAccessor implements ThreadLocalAccessor<Map<String, String>> {

    public static final String REACTOR_CONTEXT = "x-headers-context";

    @Override
    public Object key() {
        return REACTOR_CONTEXT;
    }

    @Override
    public @Nullable Map<String, String> getValue() {
        return ServletUtils.LOCAL_HEADERS.get();
    }

    @Override
    public void setValue(Map<String, String> stringStringMap) {
        ServletUtils.LOCAL_HEADERS.set(stringStringMap);
    }

    @Override
    public void setValue() {
        ServletUtils.LOCAL_HEADERS.remove();
    }

    @Override
    public void restore(Map<String, String> previousValue) {
        if (Objects.nonNull(previousValue)) {
            ServletUtils.LOCAL_HEADERS.set(previousValue);
        } else {
            ServletUtils.LOCAL_HEADERS.remove();
        }
    }
}
