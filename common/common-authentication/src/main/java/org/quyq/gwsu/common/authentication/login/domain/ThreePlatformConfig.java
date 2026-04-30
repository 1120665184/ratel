package org.quyq.gwsu.common.authentication.login.domain;


import lombok.Data;

import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/8
 * @description
 */
@Data
public class ThreePlatformConfig {

    /**
     * 客户ID
     */
    private String clientId;

    /**
     * 客户凭证
     */
    private String clientSecret;

    /**
     * 是否重定向跳转
     */
    private boolean redirect = false;

    /**
     * 重定向跳转地址
     */
    private String redirectUrl;

    /**
     * 其他扩展属性
     */
    private Map<String, String> properties;

}
