package org.quyq.gwsu.common.security.api.factory;


import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.api.IConfigInfoClientApi;
import org.quyq.gwsu.common.security.api.vo.ConfigVO;

import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/5/30
 * @description
 */
@Slf4j
public class ConfigInfoClientApiFallbackFactory implements FallbackFactory<IConfigInfoClientApi> {
    @Override
    public IConfigInfoClientApi create(Throwable cause) {
        log.error("" , cause);
        return new IConfigInfoClientApi() {
            @Override
            public R<Map<String, ConfigVO>> getByKeys(List<String> keys) {
                return R.fail(cause.getMessage());
            }
        };
    }
}
