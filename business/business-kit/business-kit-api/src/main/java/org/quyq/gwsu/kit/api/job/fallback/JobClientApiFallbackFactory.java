package org.quyq.gwsu.kit.api.job.fallback;

import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.api.job.JobClientApi;
import org.quyq.gwsu.kit.api.job.dto.JobInfoCreateDTO;
import org.springframework.stereotype.Component;

/**
 * JobClientApi 降级工厂。
 */
@Component
@Slf4j
public class JobClientApiFallbackFactory implements FallbackFactory<JobClientApi> {

    @Override
    public JobClientApi create(Throwable cause) {
        log.error(cause.getMessage(), cause);
        return new JobClientApi() {
            @Override
            public R<String> create(JobInfoCreateDTO dto) {
                return fail(cause);
            }

            @Override
            public R<String> update(JobInfoCreateDTO dto) {
                return fail(cause);
            }

            @Override
            public R<String> start(String id) {
                return fail(cause);
            }

            @Override
            public R<String> stop(String id) {
                return fail(cause);
            }

            @Override
            public R<String> remove(String id) {
                return fail(cause);
            }

            private R<String> fail(Throwable throwable) {
                return R.fail("服务暂时不可用: " + throwable.getMessage());
            }
        };
    }
}
