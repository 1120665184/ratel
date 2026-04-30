package org.quyq.gwsu.common.core.domain.visitor;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/7
 * @description 用户信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract non-sealed class UserInfo extends Visitor {

    /**
     * 用户ID
     */

    public abstract String getUserId();

    /**
     * 用户名
     */
    public abstract String getUserName();


    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class DefaultUserInfo extends UserInfo {

        private String userName;

        private String userId;

        /**
         * 扩展属性，用于存储降级反序列化时的额外字段
         */
        private Map<String, Object> prototype;

    }

}
