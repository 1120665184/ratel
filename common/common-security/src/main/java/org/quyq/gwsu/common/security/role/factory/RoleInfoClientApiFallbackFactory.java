package org.quyq.gwsu.common.security.role.factory;


import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.role.IRoleInfoClientApi;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/20
 * @description
 */
@Component
public class RoleInfoClientApiFallbackFactory implements FallbackFactory<IRoleInfoClientApi> {
    @Override
    public IRoleInfoClientApi create(Throwable cause) {
        return new IRoleInfoClientApi() {

            @Override
            public R<List<String>> getRoleListBySubject(String subjectId) {
                return R.fail(cause.getMessage());
            }
        };
    }
}
