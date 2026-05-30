package org.quyq.gwsu.common.security.api.factory;


import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.security.api.IDictInfoClientApi;

/**
 * @author Quyq
 * @date 2026/5/30
 * @description
 */
public class DictInfoClientApiFallbackFactory implements FallbackFactory<IDictInfoClientApi> {
    @Override
    public IDictInfoClientApi create(Throwable cause) {
        return null;
    }
}
