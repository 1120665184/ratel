package org.quyq.gwsu.common.authentication.login;


import org.jspecify.annotations.Nullable;
import org.quyq.gwsu.common.authentication.domain.AbstractLoginDTO;
import org.quyq.gwsu.common.authentication.exception.AuthException;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/8
 * @description 所有的登录类型的参数类型和字符串匹配
 */
public class LoginLoadingManager implements BeanPostProcessor , PriorityOrdered {


    private static final Map<String, Class<? extends AbstractLoginDTO>> LOGIN_SUPPORTS = new HashMap<>();


    public static Class<? extends AbstractLoginDTO> supportClass(String loginType, AccountType accountType) {
        return LOGIN_SUPPORTS.get(buildKey(loginType, accountType));
    }

    private static String buildKey(String loginType, AccountType accountType) {
        return "%s_%s".formatted(loginType, accountType.name());
    }

    @Override
    public @Nullable Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        if (bean instanceof LoginHandler<?> val) {
            if (LOGIN_SUPPORTS.containsKey(buildKey(val.loginType(), val.accountType()))) {
                throw new AuthException("%s 登录类型重复加载(%s账号体系)".formatted(val.loginType() , val.accountType() ));
            }
            Class<? extends AbstractLoginDTO> aClass = getFormClass(bean.getClass());
            LOGIN_SUPPORTS.put(buildKey(val.loginType(), val.accountType()), aClass);
        }
        return bean;
    }


    public Class<? extends AbstractLoginDTO> getFormClass(Class<?> target) {
        Type genericSuperclass = target.getGenericSuperclass();

        // 遍历继承链查找
        while (genericSuperclass != null) {
            if (genericSuperclass instanceof ParameterizedType parameterizedType) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                for (Type typeArg : typeArguments) {
                    if (typeArg instanceof Class<?> clazz && AbstractLoginDTO.class.isAssignableFrom(clazz)) {
                        return clazz.asSubclass(AbstractLoginDTO.class);
                    }
                }
            }
            Class<?> rawType = genericSuperclass instanceof ParameterizedType pt
                    ? (Class<?>) pt.getRawType()
                    : (Class<?>) genericSuperclass;
            genericSuperclass = rawType.getGenericSuperclass();
        }

        // 遍历接口查找
        for (Type genericInterface : target.getGenericInterfaces()) {
            Class<? extends AbstractLoginDTO> result = findAbstractLoginDTOFromInterface(genericInterface);
            if (result != null) {
                return result;
            }
        }

        throw new AuthException("无法在类 %s 的泛型参数中找到 AbstractLoginDTO 类型".formatted(target.getName()));
    }

    private Class<? extends AbstractLoginDTO> findAbstractLoginDTOFromInterface(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            for (Type typeArg : typeArguments) {
                if (typeArg instanceof Class<?> clazz && AbstractLoginDTO.class.isAssignableFrom(clazz)) {
                    return clazz.asSubclass(AbstractLoginDTO.class);
                }
            }
        }

        if (type instanceof Class<?> clazz) {
            for (Type genericInterface : clazz.getGenericInterfaces()) {
                Class<? extends AbstractLoginDTO> result = findAbstractLoginDTOFromInterface(genericInterface);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
