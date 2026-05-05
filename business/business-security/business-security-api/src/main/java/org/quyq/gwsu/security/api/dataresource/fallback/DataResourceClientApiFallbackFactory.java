package org.quyq.gwsu.security.api.dataresource.fallback;

import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.dataresource.DataResourceClientApi;
import org.quyq.gwsu.security.api.dataresource.dto.DataResourceSaveDTO;
import org.quyq.gwsu.security.api.dataresource.vo.DataResourceVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据资源配置 API 降级工厂
 *
 * @author Quyq
 * @date 2026/4/20
 */
@Component
public class DataResourceClientApiFallbackFactory implements FallbackFactory<DataResourceClientApi> {

    @Override
    public DataResourceClientApi create(Throwable cause) {
        return new DataResourceClientApi() {
            @Override
            public R<DataResourceVO> getById(Long id) {
                return R.fail("数据资源配置服务暂时不可用: " + cause.getMessage());
            }

        };
    }
}
