package org.quyq.gwsu.common.authentication.login.interceptor;

import org.quyq.gwsu.common.core.domain.visitor.UserInfo;

/**
 * 登录拦截器接口
 *
 * @author Quyq
 */
public interface LoginInterceptor<T extends UserInfo> {

    /**
     * 是否支持该登录类型
     */
    default boolean supports(String loginType) {
        return true;
    }

    /**
     * 用户认证成功后（取到用户信息后）
     *
     * @param context 拦截上下文
     * @return true 继续执行后续拦截器和登录逻辑，false 中断后续流程直接返回
     */
    default boolean afterAuthenticated(LoginInterceptorContext<T> context) {
        return true;
    }

    /**
     * 登录成功后（Token 生成后）
     *
     * @param context 拦截上下文
     */
    default void afterLoginSuccess(LoginInterceptorContext<T> context) {
    }

    /**
     * 登录失败后（异常发生时）
     *
     * @param context 拦截上下文
     * @param exception 异常信息
     */
    default void afterLoginFailure(LoginInterceptorContext<T> context, Throwable exception) {
    }

    /**
     * 执行顺序，值越小优先级越高
     */
    default int getOrder() {
        return 0;
    }
}
