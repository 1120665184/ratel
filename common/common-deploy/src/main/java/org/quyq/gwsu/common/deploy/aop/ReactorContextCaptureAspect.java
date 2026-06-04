package org.quyq.gwsu.common.deploy.aop;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Quyq
 * @date 2026/6/3
 * @description
 */
@Aspect
public class ReactorContextCaptureAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object captureReactorContext(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();

        if (result instanceof Flux<?> flux) {
            return flux.contextCapture();
        }
        if (result instanceof Mono<?> mono) {
            return mono.contextCapture();
        }

        return result;
    }
}
