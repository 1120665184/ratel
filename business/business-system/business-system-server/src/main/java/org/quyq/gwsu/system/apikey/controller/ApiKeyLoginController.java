package org.quyq.gwsu.system.apikey.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.api.IApiKeyClientApi;
import org.quyq.gwsu.common.security.api.vo.ApiKeyLoginRequest;
import org.quyq.gwsu.common.security.api.vo.ApiKeyLoginUserVO;
import org.quyq.gwsu.system.apikey.service.ISysApiKeyService;
import org.quyq.gwsu.system.login.SysUserApiKeyLoginHandler;
import org.quyq.gwsu.common.authentication.login.impl.apikey.ApiKeyLoginDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API_KEY 内部登录控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("basic/api-key")
@Tag(name = "API_KEY 内部认证")
@RequiredArgsConstructor
public class ApiKeyLoginController implements IApiKeyClientApi {

    private final SysUserApiKeyLoginHandler apiKeyLoginHandler;
    private final ISysApiKeyService apiKeyService;

    @Override
    @PostMapping("login")
    @Operation(summary = "通过 API_KEY 登录")
    public R<ApiKeyLoginUserVO> loginByApiKey(@RequestBody ApiKeyLoginRequest loginRequest) {
        ApiKeyLoginDTO loginDTO = new ApiKeyLoginDTO();
        loginDTO.setApiKey(loginRequest.getApiKey());
        loginDTO.setIp(loginRequest.getIp());
        apiKeyLoginHandler.authenticate(loginDTO);
        return R.ok(apiKeyService.validateApiKeyAndLoadUser(loginDTO.getApiKey()));
    }
}
