package org.quyq.gwsu.common.security.api.factory;


import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.api.IDictInfoClientApi;
import org.quyq.gwsu.common.security.api.vo.DictValueVO;

import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/5/30
 * @description
 */
public class DictInfoClientApiFallbackFactory implements FallbackFactory<IDictInfoClientApi> {
    @Override
    public IDictInfoClientApi create(Throwable cause) {
        return new IDictInfoClientApi() {
            @Override
            public R<Map<String, List<DictValueVO>>> getDictValueByDictKeyBatch(List<String> dictKeys) {
                return R.fail(cause.getMessage());
            }
        };
    }
}
