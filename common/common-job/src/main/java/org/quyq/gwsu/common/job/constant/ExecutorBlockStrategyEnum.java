package org.quyq.gwsu.common.job.constant;

/**
 * 执行器阻塞策略枚举
 */
public enum ExecutorBlockStrategyEnum {


    /**
     * 串行执行
     */
    SERIAL_EXECUTION("Serial execution"),

    /**
     * 丢弃后续调度
     */
    DISCARD_LATER("Discard Later"),

    /**
     * 覆盖之前调度
     */
    COVER_EARLY("Cover Early");


    private String title;

    ExecutorBlockStrategyEnum(String title) {
        this.title = title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /**
     * 根据名称匹配枚举
     *
     * @param name        枚举名称
     * @param defaultItem 默认值
     * @return 匹配的枚举项
     */
    public static ExecutorBlockStrategyEnum match(String name, ExecutorBlockStrategyEnum defaultItem) {
        if (name != null) {
            for (ExecutorBlockStrategyEnum item : ExecutorBlockStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}
