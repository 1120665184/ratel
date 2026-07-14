package org.quyq.gwsu.system.login;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.authentication.constants.AuthenticationConstants;
import org.quyq.gwsu.common.authentication.login.AbstractLoginHandler;
import org.quyq.gwsu.common.authentication.login.impl.apikey.ApiKeyLoginDTO;
import org.quyq.gwsu.system.api.manager.vo.SysUserDetailVO;
import org.quyq.gwsu.system.apikey.generator.ApiKeyTokenGenerator;
import org.quyq.gwsu.system.apikey.service.ISysApiKeyService;
import org.quyq.gwsu.system.manager.service.ISysUserService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * API_KEY 登录处理器
 *
 * @author Quyq
 */
@Component
@RequiredArgsConstructor
public class SysUserApiKeyLoginHandler extends AbstractLoginHandler<ApiKeyLoginDTO, SysUserDetailVO> {

    private final ISysApiKeyService apiKeyService;
    private final ISysUserService userService;
    private final ApiKeyTokenGenerator apiKeyTokenGenerator;

    @Override
    public String loginType() {
        return AuthenticationConstants.LoginType.API_KEY;
    }

    @Override
    protected SysUserDetailVO toAuth(ApiKeyLoginDTO loginDTO, CoreProperties properties) {
        var loginUser = apiKeyService.validateApiKeyAndLoadUser(loginDTO.getApiKey());
        SysUserDetailVO user = userService.getDetailById(loginUser.getUserId());

        SaLoginParameter loginParameter = new SaLoginParameter()
                .setToken(apiKeyTokenGenerator.normalizeToJwt(loginDTO.getApiKey()));
        if (loginUser.getExpireTime() != null) {
            long timeout = Math.max(1L, ChronoUnit.SECONDS.between(LocalDateTime.now(), loginUser.getExpireTime()));
            loginParameter.setTimeout(timeout);
        }
        properties.setSaLoginParameter(loginParameter);
        apiKeyService.refreshApiKeyLastUsed(loginUser.getApiKeyId(), loginDTO.getIp());
        return user;
    }
}
