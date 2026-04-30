package org.quyq.gwsu.common.authentication.login.interceptor;

import org.quyq.gwsu.common.core.domain.visitor.UserInfo;

import java.util.Comparator;
import java.util.List;

/**
 * 登录拦截器处理工具类
 *
 * @author Quyq
 */
public class LoginInterceptorUtils {

    private LoginInterceptorUtils() {
    }

    private static List<LoginInterceptor<UserInfo>> interceptors;

    @SuppressWarnings("unchecked")
    public static void setInterceptors(List<LoginInterceptor<?>> interceptorList) {
        LoginInterceptorUtils.interceptors = (List<LoginInterceptor<UserInfo>>) (List<?>) interceptorList;
    }

    /**
     * 触发用户认证成功事件
     *
     * @param loginType 登录类型
     * @param context   拦截上下文
     * @return true 继续执行，false 中断流程
     */
    public static boolean fireAfterAuthenticated(String loginType, LoginInterceptorContext<?> context) {
        List<LoginInterceptor<UserInfo>> sorted = sortAndFilter(loginType);

        for (LoginInterceptor<UserInfo> interceptor : sorted) {
            @SuppressWarnings("unchecked")
            LoginInterceptorContext<UserInfo> typedContext = (LoginInterceptorContext<UserInfo>) context;
            if (!interceptor.afterAuthenticated(typedContext)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 触发登录成功事件
     *
     * @param loginType 登录类型
     * @param context   拦截上下文
     */
    public static void fireAfterLoginSuccess(String loginType, LoginInterceptorContext<?> context) {
        List<LoginInterceptor<UserInfo>> sorted = sortAndFilter(loginType);

        for (LoginInterceptor<UserInfo> interceptor : sorted) {
            @SuppressWarnings("unchecked")
            LoginInterceptorContext<UserInfo> typedContext = (LoginInterceptorContext<UserInfo>) context;
            interceptor.afterLoginSuccess(typedContext);
        }
    }

    /**
     * 触发登录失败事件
     *
     * @param loginType 登录类型
     * @param context   拦截上下文
     * @param exception 异常信息
     */
    public static void fireAfterLoginFailure(String loginType, LoginInterceptorContext<?> context, Throwable exception) {
        List<LoginInterceptor<UserInfo>> sorted = sortAndFilter(loginType);

        for (LoginInterceptor<UserInfo> interceptor : sorted) {
            @SuppressWarnings("unchecked")
            LoginInterceptorContext<UserInfo> typedContext = (LoginInterceptorContext<UserInfo>) context;
            interceptor.afterLoginFailure(typedContext, exception);
        }
    }

    private static List<LoginInterceptor<UserInfo>> sortAndFilter(String loginType) {
        if (interceptors == null) {
            return List.of();
        }
        return interceptors.stream()
                .filter(i -> i.supports(loginType))
                .sorted(Comparator.comparingInt(LoginInterceptor::getOrder))
                .toList();
    }
}
