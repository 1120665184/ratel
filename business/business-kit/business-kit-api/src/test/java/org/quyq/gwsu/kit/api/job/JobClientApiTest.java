package org.quyq.gwsu.kit.api.job;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.api.job.dto.JobInfoCreateDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobClientApiTest {

    @Test
    void createAndStart_should_return_create_result_when_both_steps_succeed() {
        JobClientApi api = new JobClientApi() {
            @Override
            public R<String> create(JobInfoCreateDTO dto) {
                return R.ok("job-1");
            }

            @Override
            public R<String> update(JobInfoCreateDTO dto) {
                return R.ok();
            }

            @Override
            public R<String> start(String id) {
                return R.ok();
            }

            @Override
            public R<String> stop(String id) {
                return R.ok();
            }

            @Override
            public R<String> remove(String id) {
                return R.ok();
            }
        };

        R<String> result = api.createAndStart(new JobInfoCreateDTO());

        assertEquals(200, result.code());
        assertEquals("job-1", result.data());
    }

    @Test
    void createAndStart_should_return_start_failure_when_start_fails() {
        JobClientApi api = new JobClientApi() {
            @Override
            public R<String> create(JobInfoCreateDTO dto) {
                return R.ok("job-2");
            }

            @Override
            public R<String> update(JobInfoCreateDTO dto) {
                return R.ok();
            }

            @Override
            public R<String> start(String id) {
                return R.fail("启动失败");
            }

            @Override
            public R<String> stop(String id) {
                return R.ok();
            }

            @Override
            public R<String> remove(String id) {
                return R.ok();
            }
        };

        R<String> result = api.createAndStart(new JobInfoCreateDTO());

        assertEquals(500, result.code());
        assertEquals("启动失败", result.msg());
    }
}
