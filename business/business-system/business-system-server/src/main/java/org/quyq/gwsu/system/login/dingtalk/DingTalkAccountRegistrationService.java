package org.quyq.gwsu.system.login.dingtalk;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.authentication.constants.AuthenticationConstants;
import org.quyq.gwsu.common.authentication.exception.AuthException;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.system.api.manager.vo.SysUserDetailVO;
import org.quyq.gwsu.system.errcode.SystemErrorCode;
import org.quyq.gwsu.system.manager.domain.SysAccount;
import org.quyq.gwsu.system.manager.domain.SysUser;
import org.quyq.gwsu.system.manager.service.ISysAccountService;
import org.quyq.gwsu.system.manager.service.ISysUserService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DingTalkAccountRegistrationService {

    private static final String DINGTALK_LOGIN_TYPE = "dingtalk";
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final Pattern MD5_PASSWORD_PATTERN = Pattern.compile("^[0-9a-f]{32}$");

    private final ISysAccountService accountService;
    private final ISysUserService userService;

    @Transactional(rollbackFor = Exception.class)
    public SysUserDetailVO createAccount(String username, String password, DingTalkProfile profile) {
        username = normalizeUsername(username);
        validatePassword(password);
        if (userService.getByUsername(username) != null) {
            throw new AuthException(username, SystemErrorCode.E02001);
        }
        ensureDingTalkAccountAvailable(profile.unionId(), null);

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setNickname(profile.nick());
        user.setAvatar(profile.avatarUrl());
        user.setPhone(profile.mobile());
        user.setEmail(profile.email());
        user.setGender(0);
        user.setStatus(1);
        if (!userService.save(user)) {
            throw new AuthException(CommonErrorCode.E03006, "创建用户失败，请稍后重试");
        }

        SysAccount passwordAccount = newAccount(
                user.getId(), AuthenticationConstants.LoginType.PASSWORD, username);
        // password 已由前端 encryptPassword 做 MD5；后端与其他创建账号逻辑一致，再使用 BCrypt 落库。
        passwordAccount.setCredential(BCrypt.hashpw(password, BCrypt.gensalt()));
        saveAccount(passwordAccount);

        saveAccount(newAccount(user.getId(), DINGTALK_LOGIN_TYPE, profile.unionId()));
        return authenticatedUser(user.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public SysUserDetailVO bindAccount(String userId, DingTalkProfile profile) {
        SysUserDetailVO user = userService.getDetailById(userId);
        if (user == null) {
            throw new AuthException(userId, SystemErrorCode.E00003);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new AuthException(user.getUserName(), SystemErrorCode.E00004);
        }

        SysAccount existing = ensureDingTalkAccountAvailable(profile.unionId(), userId);
        if (existing == null) {
            saveAccount(newAccount(userId, DINGTALK_LOGIN_TYPE, profile.unionId()));
        }
        return authenticatedUser(userId);
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? null : username.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new AuthException(SystemErrorCode.E02006);
        }
        if (normalized.length() > MAX_USERNAME_LENGTH) {
            throw new AuthException(CommonErrorCode.E03006, "用户名不能超过50个字符");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new AuthException(SystemErrorCode.E02007);
        }
        if (!MD5_PASSWORD_PATTERN.matcher(password).matches()) {
            throw new AuthException(CommonErrorCode.E03006, "密码参数格式不正确");
        }
    }

    private SysAccount ensureDingTalkAccountAvailable(String unionId, String expectedUserId) {
        SysAccount existing = accountService.getByIdentifier(DINGTALK_LOGIN_TYPE, unionId);
        if (existing != null && (expectedUserId == null || !expectedUserId.equals(existing.getUserId()))) {
            throw new AuthException(CommonErrorCode.E03006, "该钉钉账号已绑定其他用户");
        }
        return existing;
    }

    private SysAccount newAccount(String userId, String identityType, String identifier) {
        SysAccount account = new SysAccount();
        account.setUserId(userId);
        account.setIdentityType(identityType);
        account.setIdentifier(identifier);
        account.setStatus(1);
        account.setVerified(true);
        account.setVerifiedTime(LocalDateTime.now());
        account.setBindTime(LocalDateTime.now());
        return account;
    }

    private void saveAccount(SysAccount account) {
        if (!accountService.save(account)) {
            throw new AuthException(CommonErrorCode.E03006, "创建登录账号失败，请稍后重试");
        }
    }

    public SysUserDetailVO authenticatedUser(String userId) {
        SysUserDetailVO detail = userService.getDetailById(userId);
        if (detail == null) {
            throw new AuthException(userId, SystemErrorCode.E00003);
        }
        detail.setLoginType(DINGTALK_LOGIN_TYPE);
        return detail;
    }

    public record DingTalkProfile(
            String nick,
            String avatarUrl,
            String mobile,
            String unionId,
            String email
    ) {
    }
}
