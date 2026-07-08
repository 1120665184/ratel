package org.quyq.gwsu.kit.job.controller;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.job.JobClientApi;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JobInfoControllerContractTest {

    @Test
    void jobInfoController_should_implement_job_client_api() {
        assertTrue(JobClientApi.class.isAssignableFrom(JobInfoController.class));
    }
}
