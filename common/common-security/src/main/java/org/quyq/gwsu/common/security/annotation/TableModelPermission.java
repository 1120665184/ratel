package org.quyq.gwsu.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表模型权限注解，标注在 Controller 类或方法上
 * <p>
 * 生效规则：
 * - 无注解：无表模型权限
 * - 仅类上：继承类上的表模型权限
 * - 仅方法上：使用方法上的
 * - 类+方法（方法有配置）：方法覆盖类
 * - 类+方法（空注解）：该接口不继承类上的表模型权限
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TableModelPermission {
    /** 表模型对应的Domain类列表（类需有@TableName注解） */
    Class<?>[] value() default {};
    /** 表名列表（直接指定表名，与value互为补充） */
    String[] tables() default {};
}
