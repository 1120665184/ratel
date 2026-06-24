package org.quyq.gwsu.system.login;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.authentication.exception.AuthException;
import org.quyq.gwsu.common.authentication.login.impl.dingtalk.DingTalkLoginHandler;
import org.quyq.gwsu.system.api.manager.vo.SysUserDetailVO;
import org.quyq.gwsu.system.errcode.SystemErrorCode;
import org.quyq.gwsu.system.manager.domain.SysAccount;
import org.quyq.gwsu.system.manager.domain.SysUser;
import org.quyq.gwsu.system.manager.service.ISysAccountService;
import org.quyq.gwsu.system.manager.service.ISysUserService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description 钉钉快捷登录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingTalkLoginHandlerImpl extends DingTalkLoginHandler<SysUserDetailVO> {

    private final ISysAccountService accountService;
    private final ISysUserService userService;

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

        // 2. 账号不存在，创建新用户并绑定钉钉账号
        log.info("[DingTalkLogin] 钉钉用户首次登录，创建新用户，unionId={}, nick={}", info.unionId(), info.nick());

        SysUser newUser = new SysUser();
        newUser.setNickname(info.nick());
        newUser.setAvatar(info.avatarUrl());
        newUser.setPhone(info.mobile());
        newUser.setEmail(info.email());
        newUser.setUsername("dingtalk_" + info.unionId());
        newUser.setGender(0);
        newUser.setStatus(1);
        userService.save(newUser);

        SysAccount newAccount = new SysAccount();
        newAccount.setUserId(newUser.getId());
        newAccount.setIdentityType(loginType());
        newAccount.setIdentifier(info.unionId());
        newAccount.setStatus(1);
        newAccount.setVerified(true);
        newAccount.setBindTime(LocalDateTime.now());
        accountService.save(newAccount);

        SysUserDetailVO userDetail = userService.getDetailById(newUser.getId());
        userDetail.setLoginType(loginType());
        return userDetail;
    }

}
