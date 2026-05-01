package org.quyq.gwsu.security.api.role.fallback;

import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.role.RoleClientApi;
import org.quyq.gwsu.security.api.role.vo.RoleVO;
import org.springframework.stereotype.Component;

/**
 * 角色 API 降级工厂
 *
 * @author Quyq
 */
@Component
public class RoleClientApiFallbackFactory implements FallbackFactory<RoleClientApi> {

    @Override
    public RoleClientApi create(Throwable cause) {
        return new RoleClientApi() {
            @Override
            public R<RoleVO> getById(String id) {
                return R.fail("角色服务暂时不可用: " + cause.getMessage());
            }
        };
    }
}
