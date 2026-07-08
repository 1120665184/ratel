package org.quyq.gwsu.common.job.openapi.executor.dto;

import java.io.Serializable;

/**
 * 终止任务请求DTO
 */
public class KillRequest implements Serializable {
    private static final long serialVersionUID = 42L;

    private String jobId;

    public KillRequest() {
    }

    public KillRequest(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

}
