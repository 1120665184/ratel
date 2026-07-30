package org.quyq.gwsu.system.login.dingtalk;


import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.authentication.domain.LoginVO;
import org.quyq.gwsu.common.authentication.login.impl.dingtalk.DingTalkLoginHandler;
import org.quyq.gwsu.common.authentication.login.interceptor.LoginInterceptor;
import org.quyq.gwsu.common.authentication.login.interceptor.LoginInterceptorContext;
import org.quyq.gwsu.system.api.manager.vo.SysUserDetailVO;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/7/30
 * @description
 */
@Component
@RequiredArgsConstructor
public class DingTalkLoginInterceptor implements LoginInterceptor<SysUserDetailVO> {

    private final DingTalkLoginHandlerImpl dingTalkLoginHandler;

    @Override
    public boolean afterAuthenticated(LoginInterceptorContext<SysUserDetailVO> context) {
        LoginVO loginVO = context.getLoginVO();
        //账号是否首次登录
        boolean nonNew = StringUtils.hasText(loginVO.getUserId());
        if (!nonNew) {
            Optional<SysUserDetailVO> userInfoOpt = context.getSubject().userInfo();
            SysUserDetailVO userInfo = userInfoOpt.orElseThrow();
            loginVO.getExtraData().put(DingTalkLoginHandler.PARAM_TEMPORARY_VOUCHER_KEY, userInfo.getUserName());
        }
        return nonNew;
    }

    @Override
    public void afterLoginSuccess(LoginInterceptorContext<SysUserDetailVO> context) {
        MultiValueMap<String, String> extraParam = context.getLoginDTO().getExtraParam();
        String createMethod = extraParam == null ? null : extraParam.getFirst(DingTalkLoginHandler.PARAM_CREATE_METHOD_KEY);
        String temporaryVoucher = extraParam == null ? null : extraParam.getFirst(DingTalkLoginHandler.PARAM_TEMPORARY_VOUCHER_KEY);
        if (StringUtils.hasText(createMethod) && StringUtils.hasText(temporaryVoucher)) {
            dingTalkLoginHandler.consumeTemporaryVoucher(temporaryVoucher);
        }
    }

    @Override
    public boolean supports(String loginType) {
        return DingTalkLoginHandler.THREAD_DINGTALK_LOGIN_TYPE.equals(loginType);
    }
}
