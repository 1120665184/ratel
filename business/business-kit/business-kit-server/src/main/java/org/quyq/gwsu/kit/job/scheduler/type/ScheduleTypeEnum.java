package org.quyq.gwsu.kit.job.scheduler.type;

import org.quyq.gwsu.kit.job.scheduler.type.strategy.CronScheduleType;
import org.quyq.gwsu.kit.job.scheduler.type.strategy.FixRateScheduleType;
import org.quyq.gwsu.kit.job.scheduler.type.strategy.NoneScheduleType;

/**
 * 调度类型枚举
 */
public enum ScheduleTypeEnum {

    NONE("无", new NoneScheduleType()),

    /**
     * Cron调度
     */
    CRON("Cron", new CronScheduleType()),

    /**
     * 固定速率调度（秒）
     */
    FIX_RATE("固定速率", new FixRateScheduleType());

    private final String title;
    private final ScheduleType scheduleType;

    ScheduleTypeEnum(String title, ScheduleType scheduleType) {
        this.title = title;
        this.scheduleType = scheduleType;
    }

    public String getTitle() {
        return title;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    /**
     * 按名称匹配
     *
     * @param name          名称
     * @param defaultItem   默认值
     * @return 匹配的枚举项
     */
    public static ScheduleTypeEnum match(String name, ScheduleTypeEnum defaultItem) {
        if (name != null) {
            for (ScheduleTypeEnum item : ScheduleTypeEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }

}
