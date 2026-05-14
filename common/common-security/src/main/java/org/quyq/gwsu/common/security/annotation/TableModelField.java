package org.quyq.gwsu.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表模型字段配置注解，标注在 Domain 类字段上
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface TableModelField {
    /** 是否允许AI查询该字段，默认true */
    boolean show() default true;
    /** 返回给用户时是否脱敏，默认false */
    boolean desensitize() default false;
    /** 脱敏策略，默认NONE */
    SensitiveStrategy strategy() default SensitiveStrategy.NONE;
    /** 自定义脱敏-不脱敏前缀长度，仅 strategy=CUSTOM 时生效 */
    int prefixNoMaskLen() default 1;
    /** 自定义脱敏-不脱敏后缀长度，仅 strategy=CUSTOM 时生效 */
    int suffixNoMaskLen() default 1;
    /** 自定义脱敏-脱敏标识符，仅 strategy=CUSTOM 时生效 */
    String symbol() default "*";
}
