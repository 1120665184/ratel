package org.quyq.gwsu.common.security.api.factory;

import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.api.IApiKeyClientApi;
import org.quyq.gwsu.common.security.api.vo.ApiKeyLoginRequest;
import org.quyq.gwsu.common.security.api.vo.ApiKeyLoginUserVO;
import org.springframework.stereotype.Component;

/**
 * API_KEY 认证降级工厂
 *
 * @author Quyq
 */
@Component
public class ApiKeyClientApiFallbackFactory implements FallbackFactory<IApiKeyClientApi> {
    @Override
    public IApiKeyClientApi create(Throwable cause) {
        return new IApiKeyClientApi() {
            @Override
            public R<ApiKeyLoginUserVO> loginByApiKey(ApiKeyLoginRequest loginRequest) {
                return R.fail(cause.getMessage());
            }
        };
    }
}
