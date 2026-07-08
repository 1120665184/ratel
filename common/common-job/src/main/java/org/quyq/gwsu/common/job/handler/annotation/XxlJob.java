package org.quyq.gwsu.common.job.handler.annotation;

import java.lang.annotation.*;

/**
 * 方法任务处理器注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface XxlJob {

    /**
     * 任务处理器名称
     */
    String value();

    /**
     * 初始化方法，当JobThread初始化时调用
     */
    String init() default "";

    /**
     * 销毁方法，当JobThread销毁时调用
     */
    String destroy() default "";

}
