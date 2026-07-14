package org.quyq.gwsu.system.apikey.generator;

import cn.dev33.satoken.strategy.SaStrategy;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * API_KEY Token 生成器
 *
 * @author Quyq
 */
@Component
public class ApiKeyTokenGenerator {

    private static final int MASK_PREFIX_LENGTH = 9;
    private static final int MASK_SUFFIX_LENGTH = 4;

    public String generateApiKey(String loginId) {
        String jwt = SaStrategy.instance.createToken.apply(loginId, AccountType.MANAGER.name());
        return SecurityConstants.Authentication.API_KEY_PREFIX + jwt;
    }

    public String mask(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return apiKey;
        }
        if (apiKey.length() <= MASK_PREFIX_LENGTH + MASK_SUFFIX_LENGTH) {
            return apiKey.substring(0, Math.min(5, apiKey.length())) + "****";
        }
        return apiKey.substring(0, MASK_PREFIX_LENGTH)
                + "***********"
                + apiKey.substring(apiKey.length() - MASK_SUFFIX_LENGTH);
    }

    public String normalizeToJwt(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return apiKey;
        }
        return apiKey.startsWith(SecurityConstants.Authentication.API_KEY_PREFIX)
                ? apiKey.substring(SecurityConstants.Authentication.API_KEY_PREFIX.length()) : apiKey;
    }
}
