package org.quyq.gwsu.common.security.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Quyq
 * @date 2026/4/17
 * @description 标注该注解的接口方法，用户认证后便可以登录
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface LoginAllowAccess {
}
