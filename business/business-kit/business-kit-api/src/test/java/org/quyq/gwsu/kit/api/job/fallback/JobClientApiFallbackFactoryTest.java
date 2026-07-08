package org.quyq.gwsu.kit.api.job.fallback;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.api.job.JobClientApi;
import org.quyq.gwsu.kit.api.job.dto.JobInfoCreateDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobClientApiFallbackFactoryTest {

    @Test
    void create_should_return_fail_result() {
        JobClientApi api = new JobClientApiFallbackFactory().create(new RuntimeException("network down"));

        R<String> result = api.create(new JobInfoCreateDTO());

        assertEquals(500, result.code());
        assertTrue(result.msg().contains("服务暂时不可用"));
        assertTrue(result.msg().contains("network down"));
    }
}
