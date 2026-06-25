package org.quyq.gwsu.security.api.menu;

import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.constants.CoreConstants;
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
@ApiClient(value = CoreConstants.Server.SECURITY_NAME, note = "菜单管理API", fallbackFactory = MenuClientApiFallbackFactory.class)
@HttpExchange("/security/menu")
public interface MenuClientApi {

    /**
     * 根据ID查询菜单
     */
    @GetExchange("/{id}")
    R<MenuVO> getById(@PathVariable("id") String id);

}
