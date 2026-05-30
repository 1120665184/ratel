package org.quyq.gwsu.common.security.api.factory;


import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.api.IRoleInfoClientApi;
import org.quyq.gwsu.common.security.api.vo.UserRoleInfo;
import org.springframework.stereotype.Component;

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
            public R<UserRoleInfo> getRoleListBySubject(String subjectId) {
                return R.fail(cause.getMessage());
            }
        };
    }
}
