package org.quyq.gwsu.common.security.api.factory;


import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.security.api.IConfigInfoClientApi;

/**
 * @author Quyq
 * @date 2026/5/30
 * @description
 */
public class ConfigInfoClientApiFallbackFactory implements FallbackFactory<IConfigInfoClientApi> {
    @Override
    public IConfigInfoClientApi create(Throwable cause) {
        return null;
    }
}
