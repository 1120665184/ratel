package org.quyq.gwsu.security.api.menu.fallback;

import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.menu.MenuClientApi;
import org.quyq.gwsu.security.api.menu.dto.MenuQueryDTO;
import org.quyq.gwsu.security.api.menu.dto.MenuSortDTO;
import org.quyq.gwsu.security.api.menu.vo.MenuVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 菜单 API 降级工厂
 *
 * @author Quyq
 */
@Component
public class MenuClientApiFallbackFactory implements FallbackFactory<MenuClientApi> {

    @Override
    public MenuClientApi create(Throwable cause) {
        return new MenuClientApi() {
            @Override
            public R<MenuVO> getById(String id) {
                return R.fail("菜单服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public R<List<MenuVO>> listTreeBySubjectId(Integer owner, String subjectId) {
                return R.fail("菜单服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public R<List<MenuVO>> listTree(MenuQueryDTO query) {
                return R.fail("菜单服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public R<List<Map<String, Object>>> listOwners() {
                return R.fail("菜单服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public R<List<Map<String, Object>>> listPositions() {
                return R.fail("菜单服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public R<List<MenuVO>> listButtonsByMenuId(Integer owner, String menuId) {
                return R.fail("菜单服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public R<Boolean> batchSort(List<MenuSortDTO> sortItems) {
                return R.fail("菜单服务暂时不可用: " + cause.getMessage());
            }
        };
    }
}
