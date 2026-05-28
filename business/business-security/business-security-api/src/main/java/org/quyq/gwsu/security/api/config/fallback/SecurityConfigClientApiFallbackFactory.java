package org.quyq.gwsu.security.api.config.fallback;

import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.config.SecurityConfigClientApi;
import org.quyq.gwsu.security.api.config.vo.ConfigVO;
import org.springframework.stereotype.Component;

/**
 * 配置管理 API 降级工厂
 *
 * @author Quyq
 */
@Component
public class SecurityConfigClientApiFallbackFactory implements FallbackFactory<SecurityConfigClientApi> {

    @Override
    public SecurityConfigClientApi create(Throwable cause) {
        return new SecurityConfigClientApi() {
            @Override
            public R<ConfigVO> getById(String id) {
                return R.fail("配置服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public R<ConfigVO> getByKey(String configKey) {
                return R.fail("配置服务暂时不可用: " + cause.getMessage());
            }
        };
    }
}
