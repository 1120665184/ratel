package org.quyq.gwsu.common.security.api;


import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.api.vo.UserRoleInfo;
import org.quyq.gwsu.common.security.api.factory.RoleInfoClientApiFallbackFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Quyq
 * @date 2026/4/20
 * @description
 */
@ApiClient(value = CoreConstants.Server.SECURITY_NAME, note = "角色信息获取", fallbackFactory = RoleInfoClientApiFallbackFactory.class)
@HttpExchange("/role")
public interface IRoleInfoClientApi {

    /**
     * 通过主体ID获取关联的角色
     *
     * @param subjectId
     * @return
     */
    @GetExchange("/list/{subjectId}")
    R<UserRoleInfo> getRoleListBySubject(@PathVariable String subjectId);

}
