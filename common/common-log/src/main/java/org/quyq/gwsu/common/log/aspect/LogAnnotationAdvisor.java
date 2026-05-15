package org.quyq.gwsu.common.log.aspect;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

public class LogAnnotationAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

    private final transient Advice advice;

    private final transient Pointcut pointcut;

    public LogAnnotationAdvisor(LogAspectInterceptor advice) {
        this.advice = advice;
        this.pointcut = buildPointcut();
    }

    private Pointcut buildPointcut() {
        Pointcut cpc = new AnnotationMatchingPointcut(RestController.class, true);
        return new ComposablePointcut(cpc);
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (this.advice instanceof BeanFactoryAware) {
            ((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogAnnotationAdvisor)) return false;
        if (!super.equals(o)) return false;
        LogAnnotationAdvisor advisor = (LogAnnotationAdvisor) o;
        return Objects.equals(advice, advisor.advice) && Objects.equals(pointcut, advisor.pointcut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), advice, pointcut);
    }
}
