package org.quyq.gwsu.common.cache.utils.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * @author Quyq
 * @date 2024/5/9
 * @description 基于redission实现的分布式锁
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    String value() ; // 锁的名称或标识符
    long waitTime() default 10; // 等待获取锁的时间
    long leaseTime() default -1; // 锁的租赁时间，-1表示无限期
    TimeUnit timeUnit() default TimeUnit.SECONDS; // 时间单位 秒

}
