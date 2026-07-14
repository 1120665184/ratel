package org.quyq.gwsu.common.security.api;

import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.api.factory.ApiKeyClientApiFallbackFactory;
import org.quyq.gwsu.common.security.api.vo.ApiKeyLoginRequest;
import org.quyq.gwsu.common.security.api.vo.ApiKeyLoginUserVO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * API_KEY 认证远程接口
 *
 * @author Quyq
 */
@ApiClient(value = CoreConstants.Server.SYSTEM_NAME, note = "API_KEY 认证", fallbackFactory = ApiKeyClientApiFallbackFactory.class)
@HttpExchange("basic/api-key")
public interface IApiKeyClientApi {

    /**
     * 通过 API_KEY 登录并建立标准会话
     *
     * @param loginRequest API_KEY 登录参数
     * @return 登录用户信息
     */
    @PostExchange("login")
    R<ApiKeyLoginUserVO> loginByApiKey(@RequestBody ApiKeyLoginRequest loginRequest);
}
