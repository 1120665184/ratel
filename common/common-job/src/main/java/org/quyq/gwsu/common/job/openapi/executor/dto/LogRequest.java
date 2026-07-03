package org.quyq.gwsu.common.job.openapi.executor.dto;

import java.io.Serializable;

/**
 * 日志查询请求DTO
 */
public class LogRequest implements Serializable {
    private static final long serialVersionUID = 42L;

    private String logId;
    private long logDateTime;
    private int fromLineNum;

    public LogRequest() {
    }

    public LogRequest(String logId, long logDateTime, int fromLineNum) {
        this.logId = logId;
        this.logDateTime = logDateTime;
        this.fromLineNum = fromLineNum;
    }

    public long getLogDateTime() {
        return logDateTime;
    }

    public void setLogDateTime(long logDateTime) {
        this.logDateTime = logDateTime;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public int getFromLineNum() {
        return fromLineNum;
    }

    public void setFromLineNum(int fromLineNum) {
        this.fromLineNum = fromLineNum;
    }

}
