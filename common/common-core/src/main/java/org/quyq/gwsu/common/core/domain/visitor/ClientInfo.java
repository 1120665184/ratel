package org.quyq.gwsu.common.core.domain.visitor;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/7
 * @description 客户端信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract non-sealed class ClientInfo extends Visitor {


    /**
     * 获取客户端ID
     *
     * @return
     */
    public abstract String getClientId();

    /**
     * 获取客户端凭证
     *
     * @return
     */
    public abstract String getClientSecret();

    /**
     * 获取客户端名称
     * @return
     */
    public abstract String getClientName();


    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class DefaultClientInfo extends ClientInfo {

        private String clientId;

        private String clientName;

        private String clientSecret;

        /**
         * 扩展属性，用于存储降级反序列化时的额外字段
         */
        private Map<String, Object> prototype;
    }

}
