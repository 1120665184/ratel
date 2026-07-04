package org.quyq.gwsu.kit.job.handler;


import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.job.context.XxlJobHelper;
import org.quyq.gwsu.common.job.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

/**
 * @author Quyq
 * @date 2026/7/4
 * @description
 */

@Component
@Slf4j
public class TestHandler {

    @XxlJob("testHandler")
    public void testJob() throws Exception {
        String jobId = XxlJobHelper.getJobId();
        String jobParam = XxlJobHelper.getJobParam();
        log.info("任务开始：jobId={}, jobParam={}", jobId, jobParam);
        XxlJobHelper.log("jobId: " + jobId + ", jobParam: " + jobParam);
    }

}
