package org.quyq.gwsu.common.job.glue.impl;

import org.quyq.gwsu.common.job.executor.XxlJobExecutor;
import org.quyq.gwsu.common.job.glue.GlueFactory;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Spring Glue工厂，支持Spring Bean注入
 */
public class SpringGlueFactory extends GlueFactory {
    private static final Logger logger = LoggerFactory.getLogger(SpringGlueFactory.class);

    /**
     * 注入Spring Bean
     *
     * @param instance 实例
     */
    @Override
    public void injectService(Object instance) {
        if (instance == null) {
            return;
        }

        if (XxlJobExecutor.getApplicationContext() == null) {
            return;
        }

        Field[] fields = instance.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Object fieldBean = null;

            if (AnnotationUtils.getAnnotation(field, Resource.class) != null) {
                try {
                    Resource resource = AnnotationUtils.getAnnotation(field, Resource.class);
                    if (resource != null && !resource.name().isEmpty()) {
                        fieldBean = XxlJobExecutor.getApplicationContext().getBean(resource.name());
                    } else {
                        fieldBean = XxlJobExecutor.getApplicationContext().getBean(field.getName());
                    }
                } catch (Exception e) {
                    // ignore
                }
                if (fieldBean == null) {
                    fieldBean = XxlJobExecutor.getApplicationContext().getBean(field.getType());
                }
            } else if (AnnotationUtils.getAnnotation(field, Autowired.class) != null) {
                Qualifier qualifier = AnnotationUtils.getAnnotation(field, Qualifier.class);
                if (qualifier != null && !qualifier.value().isEmpty()) {
                    fieldBean = XxlJobExecutor.getApplicationContext().getBean(qualifier.value());
                } else {
                    fieldBean = XxlJobExecutor.getApplicationContext().getBean(field.getType());
                }
            }

            if (fieldBean != null) {
                field.setAccessible(true);
                try {
                    field.set(instance, fieldBean);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

}
