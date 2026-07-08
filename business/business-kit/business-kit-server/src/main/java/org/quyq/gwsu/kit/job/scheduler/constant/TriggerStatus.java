package org.quyq.gwsu.kit.job.scheduler.constant;

/**
 * 触发状态
 */
public enum TriggerStatus {

    /**
     * 已停止
     */
    STOPPED(0, "已停止"),

    /**
     * 运行中
     */
    RUNNING(1, "运行中");

    private int value;
    private String desc;

    TriggerStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

}
