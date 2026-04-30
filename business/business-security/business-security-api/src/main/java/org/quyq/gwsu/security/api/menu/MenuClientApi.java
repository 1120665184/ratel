package org.quyq.gwsu.security.api.menu;

import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.menu.fallback.MenuClientApiFallbackFactory;
import org.quyq.gwsu.security.api.menu.vo.MenuVO;
import org.quyq.gwsu.security.api.menu.dto.MenuQueryDTO;
import org.quyq.gwsu.security.api.menu.dto.MenuSortDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.List;
import java.util.Map;

/**
 * 菜单 API 客户端接口
 *
 * @author Quyq
 */
@ApiClient(value = "gwsu-security", note = "菜单管理API", fallbackFactory = MenuClientApiFallbackFactory.class)
@HttpExchange("/security/menu")
public interface MenuClientApi {

    /**
     * 根据ID查询菜单
     */
    @GetExchange("/{id}")
    R<MenuVO> getById(@PathVariable("id") String id);

    /**
     * 根据用户ID查询菜单树
     */
    @GetExchange("/tree/{owner}/by-subject/{subjectId}")
    R<List<MenuVO>> listTreeBySubjectId(
            @PathVariable("owner") Integer owner,
            @PathVariable("subjectId") String subjectId);

    /**
     * 查询菜单树
     */
    @PostExchange("/tree")
    R<List<MenuVO>> listTree(MenuQueryDTO query);

    /**
     * 获取菜单所属类型枚举
     */
    @GetExchange("/enums/owners")
    R<List<Map<String, Object>>> listOwners();

    /**
     * 获取菜单位置类型枚举
     */
    @GetExchange("/enums/positions")
    R<List<Map<String, Object>>> listPositions();

    /**
     * 获取指定菜单下的按钮列表
     */
    @GetExchange("/tree/{owner}/buttons/{menuId}")
    R<List<MenuVO>> listButtonsByMenuId(
            @PathVariable("owner") Integer owner,
            @PathVariable("menuId") String menuId);

    /**
     * 批量更新菜单排序和父级
     */
    @PutExchange("/sort")
    R<Boolean> batchSort(List<MenuSortDTO> sortItems);
}
