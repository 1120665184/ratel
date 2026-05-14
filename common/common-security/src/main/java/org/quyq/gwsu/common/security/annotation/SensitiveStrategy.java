package org.quyq.gwsu.common.security.annotation;

/**
 * 脱敏策略枚举
 */
public enum SensitiveStrategy {
    /** 无脱敏 */
    NONE,
    /** 用户名：张** */
    USERNAME,
    /** 身份证：3301**********1234 */
    ID_CARD,
    /** 手机号：138****1234 */
    PHONE,
    /** 邮箱：a****b@example.com */
    EMAIL,
    /** 地址：浙江省****杭州市**** */
    ADDRESS,
    /** 自定义脱敏 */
    CUSTOM
}
