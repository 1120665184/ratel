package org.quyq.gwsu.system.login.dingtalk;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.authentication.exception.AuthException;
import org.quyq.gwsu.common.authentication.constants.AuthenticationConstants;
import org.quyq.gwsu.common.authentication.login.impl.dingtalk.DingTalkLoginHandler;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.system.api.manager.vo.SysUserDetailVO;
import org.quyq.gwsu.system.errcode.SystemErrorCode;
import org.quyq.gwsu.system.manager.domain.SysAccount;
import org.quyq.gwsu.system.manager.service.ISysAccountService;
import org.quyq.gwsu.system.manager.service.ISysUserService;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description 钉钉快捷登录
 */
@Component
@RequiredArgsConstructor
public class DingTalkLoginHandlerImpl extends DingTalkLoginHandler<SysUserDetailVO> {

    private static final String TEMPORARY_VOUCHER_KEY = "dingtalk:voucher:";

    private final ISysAccountService accountService;
    private final ISysUserService userService;

    private final CacheUtils cacheUtils;
    private final SecurityUtils securityUtils;
    private final DingTalkAccountRegistrationService registrationService;


    @Override
    protected SysUserDetailVO getUserByDingTalkInfo(DingTalkInfo info) {
        // 1. 通过 unionId + 钉钉登录类型 查找已有账号
        SysAccount account = accountService.getByIdentifier(loginType(), info.unionId());

        if (account != null) {
            // 账号已存在，检查状态
            if (account.getStatus() == null || account.getStatus() != 1) {
                throw new AuthException(info.nick(), SystemErrorCode.E00002);
            }
            // 通过 userId 查询用户详情
            SysUserDetailVO user = userService.getDetailById(account.getUserId());
            if (user == null) {
                throw new AuthException(info.nick(), SystemErrorCode.E00003);
            }
            if (user.getStatus() == null || user.getStatus() != 1) {
                throw new AuthException(user.getUserName(), SystemErrorCode.E00004);
            }
            user.setLoginType(loginType());
            return user;
        }

        //临时凭证
        String temporaryVoucher = IdUtil.simpleUUID();
        cacheUtils.set(TEMPORARY_VOUCHER_KEY + temporaryVoucher, new DingTalkVoucher(info, null), 5, TimeUnit.MINUTES);
        SysUserDetailVO user = new SysUserDetailVO();
        user.setUserName(temporaryVoucher);
        return user;
    }

    @Override
    protected SysUserDetailVO createNewAccount(String temporaryVoucher, MultiValueMap<String, String> extraParam) {
        return completeWithVoucher(temporaryVoucher,
                info -> registrationService.createAccount(
                        requiredParam(extraParam, PARAM_USERNAME_KEY),
                        requiredParam(extraParam, PARAM_PASSWORD_KEY),
                        toProfile(info)));
    }

    @Override
    protected SysUserDetailVO bindingUser(String temporaryVoucher, MultiValueMap<String, String> extraParam) {
        String bindingToken = requiredParam(extraParam, PARAM_BINDING_TOKEN_KEY);
        String loginType = securityUtils.loginType(bindingToken)
                .orElseThrow(() -> new AuthException(CommonErrorCode.E03006, "绑定登录已失效，请重新验证密码"));
        if (!AuthenticationConstants.LoginType.PASSWORD.equals(loginType)) {
            throw new AuthException(CommonErrorCode.E03006, "绑定时仅支持账号密码验证");
        }
        SysUserDetailVO user = securityUtils.<SysUserDetailVO>userInfo(bindingToken)
                .orElseThrow(() -> new AuthException(CommonErrorCode.E03006, "绑定登录已失效，请重新验证密码"));
        if (!StringUtils.hasText(user.getUserId())) {
            throw new AuthException(CommonErrorCode.E03006, "绑定登录用户信息不完整，请重新验证密码");
        }
        return completeWithVoucher(temporaryVoucher,
                info -> registrationService.bindAccount(user.getUserId(), toProfile(info)));
    }

    private SysUserDetailVO completeWithVoucher(
            String temporaryVoucher,
            Function<DingTalkInfo, SysUserDetailVO> action
    ) {
        String voucherKey = TEMPORARY_VOUCHER_KEY + temporaryVoucher;
        return cacheUtils.executeWithLock(voucherKey, () -> {
            DingTalkVoucher voucher = cacheUtils.get(voucherKey);
            if (Objects.isNull(voucher) || Objects.isNull(voucher.info())) {
                throw new AuthException(CommonErrorCode.E03006, "临时凭证已失效，请重新登录");
            }
            if (StringUtils.hasText(voucher.completedUserId())) {
                return registrationService.authenticatedUser(voucher.completedUserId());
            }
            SysUserDetailVO result = action.apply(voucher.info());
            cacheUtils.set(voucherKey, new DingTalkVoucher(voucher.info(), result.getUserId()), 5, TimeUnit.MINUTES);
            return result;
        });
    }

    public void consumeTemporaryVoucher(String temporaryVoucher) {
        if (!StringUtils.hasText(temporaryVoucher)) {
            return;
        }
        cacheUtils.delete(TEMPORARY_VOUCHER_KEY + temporaryVoucher);
    }

    private DingTalkAccountRegistrationService.DingTalkProfile toProfile(DingTalkInfo info) {
        return new DingTalkAccountRegistrationService.DingTalkProfile(
                info.nick(), info.avatarUrl(), info.mobile(), info.unionId(), info.email());
    }

    private record DingTalkVoucher(DingTalkInfo info, String completedUserId) {}

}
