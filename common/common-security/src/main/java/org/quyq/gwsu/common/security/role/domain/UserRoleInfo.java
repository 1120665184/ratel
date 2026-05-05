package org.quyq.gwsu.common.security.role.domain;


import org.jspecify.annotations.Nullable;
import org.quyq.gwsu.common.security.enums.DataScope;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/5/5
 * @description
 */
public record UserRoleInfo(
        /**
         * 拥有的数据作用域
         */
        @Nullable
        DataScope dataScope ,
        /**
         * 拥有的角色标识列表
         */
        @Nullable
        List<String> roles
) {
}
