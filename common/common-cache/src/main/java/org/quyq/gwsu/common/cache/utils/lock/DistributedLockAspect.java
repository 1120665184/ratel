package org.quyq.gwsu.common.cache.utils.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.quyq.gwsu.common.cache.exceptions.CacheException;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Quyq
 * @date 2024/5/9
 * @description 基于redission实现的分布式锁
 */
@Slf4j
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // 设置切面的优先级为最高
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final CacheUtils cacheUtils;

    /**
     * 用于SpEL表达式解析
     */
    private final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();


    /**
     * 用于获取方法参数定义名字
     */
    private final DefaultParameterNameDiscoverer defaultParameterNameDiscoverer = new DefaultParameterNameDiscoverer();


    @Around("@annotation(distributedLock)")
    public Object applyLock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String spel = distributedLock.value();
        String keyBySpeL = getKeyBySpeL(spel, joinPoint);
        log.trace("分布式锁触发，key:{}", keyBySpeL);
        long waitTime = distributedLock.waitTime();
        long leaseTime = distributedLock.leaseTime();
        TimeUnit timeUnit = distributedLock.timeUnit();
        return cacheUtils.withRebel(() ->
                cacheUtils.executeWithLock(keyBySpeL, waitTime, leaseTime, timeUnit, () -> {
                    try {
                        return joinPoint.proceed();
                    } catch (Throwable e) {
                        throw new CacheException(e);
                    }
                })
        );

    }

    /**
     * 获取缓存的value
     * value 定义在注解上，支持SPEL表达式
     *
     * @return String
     */
    public String getKeyBySpeL(String spel, ProceedingJoinPoint proceedingJoinPoint) {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        String[] paramNames = defaultParameterNameDiscoverer.getParameterNames(methodSignature.getMethod());
        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = proceedingJoinPoint.getArgs();
        if (Objects.nonNull(paramNames) && paramNames.length == args.length) {
            for (int i = 0; i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        String className = proceedingJoinPoint.getThis().getClass().getName() + ":";

        return "DISTRIBUTED_LOCK:" + className + spelExpressionParser.parseExpression(spel).getValue(context);
    }

}
