package org.quyq.gwsu.kit.job.scheduler.trigger;

/**
 * 触发类型枚举
 */
public enum TriggerTypeEnum {

    MANUAL("手动触发"),
    CRON("Cron触发"),
    RETRY("失败重试"),
    PARENT("父任务触发"),
    API("API触发"),
    MISFIRE("调度补偿");

    private final String title;

    TriggerTypeEnum(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

}
