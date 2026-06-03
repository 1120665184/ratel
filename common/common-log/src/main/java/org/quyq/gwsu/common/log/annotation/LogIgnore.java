package org.quyq.gwsu.common.log.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Quyq
 * @date 2026/6/3
 * @description 标注有该注解的接口不记录日志
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface LogIgnore {
}
