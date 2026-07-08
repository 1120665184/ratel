package org.quyq.gwsu.common.job.openapi.executor.dto;

import java.io.Serializable;

/**
 * 日志数据DTO
 */
public class LogData implements Serializable {
    private static final long serialVersionUID = 42L;

    private int fromLineNum;
    private int toLineNum;
    private String logContent;
    private boolean isEnd;

    public LogData() {
    }

    public LogData(int fromLineNum, int toLineNum, String logContent, boolean isEnd) {
        this.fromLineNum = fromLineNum;
        this.toLineNum = toLineNum;
        this.logContent = logContent;
        this.isEnd = isEnd;
    }

    public int getFromLineNum() {
        return fromLineNum;
    }

    public void setFromLineNum(int fromLineNum) {
        this.fromLineNum = fromLineNum;
    }

    public int getToLineNum() {
        return toLineNum;
    }

    public void setToLineNum(int toLineNum) {
        this.toLineNum = toLineNum;
    }

    public String getLogContent() {
        return logContent;
    }

    public void setLogContent(String logContent) {
        this.logContent = logContent;
    }

    public boolean isEnd() {
        return isEnd;
    }

    public void setEnd(boolean end) {
        isEnd = end;
    }
}
