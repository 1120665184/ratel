package org.quyq.gwsu.security.api.dict.fallback;

import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.dict.SecurityDictClientApi;
import org.quyq.gwsu.security.api.dict.vo.DictVO;
import org.springframework.stereotype.Component;

/**
 * 字典管理 API 降级工厂
 *
 * @author Quyq
 */
@Component
public class SecurityDictClientApiFallbackFactory implements FallbackFactory<SecurityDictClientApi> {

    @Override
    public SecurityDictClientApi create(Throwable cause) {
        return new SecurityDictClientApi() {
            @Override
            public R<DictVO> getById(String id) {
                return R.fail("字典服务暂时不可用: " + cause.getMessage());
            }
        };
    }
}
