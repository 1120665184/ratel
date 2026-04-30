package org.quyq.gwsu.common.core.exception.errcode;


import java.lang.annotation.*;

/**
 * @author Quyq
 * @date 2026/3/23
 * @description 错误码元信息配置 ， 注解在继承了ReturnCode接口的枚举上
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ErrorCodeMeta {


    /**
     * 错误码所属模块标识。
     * @return
     */
    String moduleCode();

    /**
     * 注释
     * @return
     */
    String notes() default "";

}
