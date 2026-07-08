package org.quyq.gwsu.common.job.openapi.executor.dto;

import java.io.Serializable;

/**
 * 空闲检测请求DTO
 */
public class IdleBeatRequest implements Serializable {
    private static final long serialVersionUID = 42L;

    private String jobId;

    public IdleBeatRequest() {
    }

    public IdleBeatRequest(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

}
