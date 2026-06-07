package org.quyq.gwsu.common.api.resolver;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;
import org.springframework.web.service.invoker.HttpRequestValues;

import java.beans.PropertyDescriptor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Objects;

public class MultipartDtoArgumentResolver implements HttpServiceArgumentResolver {

    private static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean resolve(Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
        if (argument == null) {
            return false;
        }

        if (!parameter.hasParameterAnnotation(ModelAttribute.class)) {
            return false;
        }

        BeanWrapper beanWrapper = new BeanWrapperImpl(argument);
        for (PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
            String name = pd.getName();
            if ("class".equals(name)) {
                continue;
            }
            Object value = beanWrapper.getPropertyValue(name);
            if (Objects.isNull(value)) {
                continue;
            }
            addPart(requestValues, name, value);
        }

        return true;
    }

    private void addPart(HttpRequestValues.Builder requestValues, String name, Object value) {
        if (value instanceof MultipartFile mf) {
            if (!mf.isEmpty()) {
                requestValues.addRequestPart(name, new MultipartFileResource(mf));
            }
        } else if (value instanceof Enum<?> enumValue) {
            requestValues.addRequestPart(name, enumValue.name());
        } else if (value instanceof LocalDateTime ldt) {
            requestValues.addRequestPart(name, DEFAULT_DATETIME_FORMATTER.format(ldt));
        } else if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                requestValues.addRequestPart(name, item != null ? item.toString() : "");
            }
        } else {
            requestValues.addRequestPart(name, value.toString());
        }
    }

}
