package org.quyq.gwsu.common.authentication.login.impl.headless;


import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.authentication.exception.AuthException;
import org.quyq.gwsu.common.authentication.login.AbstractLoginHandler;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.springframework.util.StringUtils;

/**
 * @author Quyq
 * @date 2026/6/13
 * @description 无头认证快速登录
 */
@RequiredArgsConstructor
public abstract class HeadlessLoginHandler<U extends UserInfo> extends AbstractLoginHandler<HeadlessLoginDTO, U> {

    private final CacheUtils cacheUtils;

    @Override
    protected U toAuth(HeadlessLoginDTO loginVO, CoreProperties properties) {
        String certificationKey = loginVO.getCertificationKey();
        if (!StringUtils.hasText(certificationKey)) {
            throw new AuthException(CommonErrorCode.E03004);
        }
        String userId = cacheUtils.withRebel(() -> cacheUtils.get(SecurityConstants.Authentication.HEADLESS_LOGIN_CERTIFICATION_CACHE_PREFIX + certificationKey));
        if (!StringUtils.hasText(userId)) {
            throw new AuthException(CommonErrorCode.E03004);
        }
        return getUserInfo(userId);
    }

    protected abstract U getUserInfo(String userId);


    @Override
    public String loginType() {
        return HeadlessLoginDTO.LOGIN_TYPE;
    }
}
