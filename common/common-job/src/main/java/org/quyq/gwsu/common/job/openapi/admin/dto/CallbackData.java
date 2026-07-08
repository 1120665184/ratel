package org.quyq.gwsu.common.job.openapi.admin.dto;

import java.io.Serializable;

/**
 * 回调数据DTO
 */
public class CallbackData implements Serializable {
    private static final long serialVersionUID = 42L;

    private String logId;
    private long logDateTime;

    private int handleCode;
    private String handleMsg;

    public CallbackData() {
    }

    public CallbackData(String logId, long logDateTime, int handleCode, String handleMsg) {
        this.logId = logId;
        this.logDateTime = logDateTime;
        this.handleCode = handleCode;
        this.handleMsg = handleMsg;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public long getLogDateTime() {
        return logDateTime;
    }

    public void setLogDateTime(long logDateTime) {
        this.logDateTime = logDateTime;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public String getHandleMsg() {
        return handleMsg;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }

    @Override
    public String toString() {
        return "CallbackData{" +
                "logId='" + logId + '\'' +
                ", logDateTime=" + logDateTime +
                ", handleCode=" + handleCode +
                ", handleMsg='" + handleMsg + '\'' +
                '}';
    }

}
