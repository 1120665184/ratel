package org.quyq.gwsu.security.api.role;

import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.role.fallback.RoleClientApiFallbackFactory;
import org.quyq.gwsu.security.api.role.vo.RoleVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * 角色 API 客户端接口
 *
 * @author Quyq
 */
@ApiClient(value = "gwsu-security", note = "角色管理API", fallbackFactory = RoleClientApiFallbackFactory.class)
@HttpExchange("/security/role")
public interface RoleClientApi {

    /**
     * 根据ID查询角色
     */
    @GetExchange("/{id}")
    R<RoleVO> getById(@PathVariable("id") String id);

    /**
     * 根据角色编码查询
     */
    @GetExchange("/code/{roleCode}")
    R<RoleVO> getByCode(@PathVariable("roleCode") String roleCode);

    /**
     * 根据主体ID查询角色列表
     */
    @GetExchange("/by-subject/{subjectId}")
    R<List<RoleVO>> listBySubjectId(@PathVariable("subjectId") String subjectId);

    /**
     * 分配角色菜单
     */
    @PostExchange("/{roleId}/menus")
    R<Boolean> assignMenus(@PathVariable("roleId") String roleId, @RequestBody List<String> menuIds);
}
