package org.quyq.gwsu.common.authentication.login;


import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import lombok.Data;
import org.jspecify.annotations.NonNull;
import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.common.authentication.constants.AuthenticationConstants;
import org.quyq.gwsu.common.authentication.dataresource.DataResourceScopeManager;
import org.quyq.gwsu.common.authentication.domain.AbstractLoginDTO;
import org.quyq.gwsu.common.authentication.domain.LoginVO;
import org.quyq.gwsu.common.authentication.domain.WorkspaceInfo;
import org.quyq.gwsu.common.authentication.login.interceptor.LoginInterceptorContext;
import org.quyq.gwsu.common.authentication.login.interceptor.LoginInterceptorUtils;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.exception.ExceptionMsgHandler;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.enums.VisitorType;
import org.quyq.gwsu.common.security.role.IRoleInfoClientApi;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/7
 * @description
 */
public abstract class AbstractLoginHandler<T extends AbstractLoginDTO, U extends UserInfo> implements LoginHandler<T> {

    /**
     * 认证逻辑
     *
     * @param loginVO
     * @param properties 其他配置属性，用于不同实现修改行为
     * @return
     */
    protected abstract U toAuth(T loginVO, CoreProperties properties);

    @Override
    public LoginVO authenticate(T loginDTO, @NonNull VisitorType visitorType) {
        CoreProperties properties = new CoreProperties();
        LoginVO loginVO = new LoginVO();
        String type = loginType();

        return ScopedValue.where(AuthenticationConstants.ACCOUNT_TYPE, accountType())
                .call(() -> {
                    LoginInterceptorContext<U> context = new LoginInterceptorContext<>(loginDTO, loginVO);

                    try {
                        U auth = toAuth(loginDTO, properties);

                        Subject<U> subject = buildSubject(auth, visitorType);
                        context.setSubject(subject);

                        loginVO.setUserId(auth.getUserId());
                        loginVO.setNeedRedirect(properties.redirect);
                        loginVO.setRedirectUrl(properties.redirectUrl);

                        // 阶段1: 用户认证成功后，判断是否继续
                        if (!LoginInterceptorUtils.fireAfterAuthenticated(type, context)) {
                            return loginVO;
                        }

                        // 登录
                        StpUtil.login("%s:%s".formatted(visitorType.name(), loginVO.getUserId()), new SaLoginParameter());
                        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

                        loginVO.setToken(tokenInfo.tokenValue);
                        loginVO.setExpires(tokenInfo.tokenTimeout);

                        putSessionData(auth, subject);


                        // 阶段2: 登录成功后
                        LoginInterceptorUtils.fireAfterLoginSuccess(type, context);

                    } catch (Exception e) {
                        // 阶段3: 登录失败后
                        LoginInterceptorUtils.fireAfterLoginFailure(type, context, e);

                        if (!properties.isRedirect()) {
                            throw e;
                        }
                        // 如果需要重定向，则不能抛出异常，将错误消息收集，重定向到对应接口上
                        ExceptionMsgHandler.ErrorInfo errorInfo = ExceptionMsgHandler.determineErrorInfo(e);
                        loginVO.setErrMsg(errorInfo.result().msg());
                        loginVO.setErrCode(errorInfo.result().errCode());
                    }

                    return loginVO;
                });


    }


    private void putSessionData(U auth, Subject<U> subject) {
        // 加载数据资源信息
        List<WorkspaceInfo> workspaceList = DataResourceScopeManager.workspaceList(auth);
        //账号session存储用户信息
        StpUtil.getSession()
                .set(SecurityConstants.Session.SESSION_SUBJECT_INFO_KEY, subject);

        // 创建TokenSession
        SaSession tokenSession = StpUtil.getTokenSession();
        // tokensession 存储当前工作空间和数据资源信息
        tokenSession.set(SecurityConstants.Session.SESSION_CURR_WORKSPACE, workspaceList.getFirst());
        tokenSession.set(SecurityConstants.Session.SESSION_CURR_DATA_RESOURCE,
                DataResourceScopeManager.dataResource(workspaceList.getFirst(), auth));
    }


    private Subject<U> buildSubject(U user, VisitorType visitorType) {
        Subject<U> subject = new Subject<>(visitorType, user, loginType());

        // 加载角色信息
        List<IRoleInfoClientApi> roleClientApi = SpringUtils.getBeansOfType(IRoleInfoClientApi.class);
        if (!CollectionUtils.isEmpty(roleClientApi)) {
            List<String> roles = FeignUtils.data(roleClientApi.getFirst().getRoleListBySubject(user.getUserId()));
            if (!CollectionUtils.isEmpty(roles)) {
                subject.setRoles(roles);
            }
        }

        return subject;
    }

    @Data
    protected static class CoreProperties {

        /**
         * 是否重定向跳转
         */
        private boolean redirect = false;

        /**
         * 重定向跳转地址
         */
        private String redirectUrl;

    }

}
