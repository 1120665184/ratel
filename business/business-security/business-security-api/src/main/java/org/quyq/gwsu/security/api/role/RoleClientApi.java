package org.quyq.gwsu.security.api.role;

import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.role.fallback.RoleClientApiFallbackFactory;
import org.quyq.gwsu.security.api.role.vo.RoleVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 角色 API 客户端接口
 *
 * @author Quyq
 */
@ApiClient(value = CoreConstants.Server.SECURITY_NAME, note = "角色管理API", fallbackFactory = RoleClientApiFallbackFactory.class)
@HttpExchange("/role")
public interface RoleClientApi {

    /**
     * 根据ID查询角色
     */
    @GetExchange("/{id}")
    R<RoleVO> getById(@PathVariable String id);

}
