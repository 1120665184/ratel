package org.quyq.gwsu.common.job.openapi.executor.dto;

import java.io.Serializable;

/**
 * 终止任务请求DTO
 */
public class KillRequest implements Serializable {
    private static final long serialVersionUID = 42L;

    private int jobId;

    public KillRequest() {
    }

    public KillRequest(int jobId) {
        this.jobId = jobId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

}
