package org.quyq.gwsu.common.authentication.login.domain;


import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/4/8
 * @description 返回给前端的授权url信息
 */
public record WebCallInfo(
        @NonNull
        String url,
        Map<String, Object> extraData
) {
    public WebCallInfo {
        if (Objects.isNull(extraData)) {
            extraData = new HashMap<>();
        }
    }

}
