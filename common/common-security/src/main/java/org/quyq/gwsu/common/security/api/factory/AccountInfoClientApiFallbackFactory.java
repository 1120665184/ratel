package org.quyq.gwsu.common.security.api.factory;


import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.api.IAccountInfoClientApi;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description
 */
@Slf4j
public class AccountInfoClientApiFallbackFactory implements FallbackFactory<IAccountInfoClientApi> {
    @Override
    public IAccountInfoClientApi create(Throwable cause) {
        log.error("", cause);
        return new IAccountInfoClientApi() {
            @Override
            public R<String> getUserIdByDingTalkUnionId(String unionId) {
                return R.fail(cause.getMessage());
            }
        };
    }
}
