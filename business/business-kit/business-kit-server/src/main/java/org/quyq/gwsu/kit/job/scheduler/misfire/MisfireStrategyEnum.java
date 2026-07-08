package org.quyq.gwsu.kit.job.scheduler.misfire;

import org.quyq.gwsu.kit.job.scheduler.misfire.strategy.MisfireDoNothing;
import org.quyq.gwsu.kit.job.scheduler.misfire.strategy.MisfireFireOnceNow;

/**
 * 调度过期策略枚举
 */
public enum MisfireStrategyEnum {

    /**
     * 忽略
     */
    DO_NOTHING("忽略", new MisfireDoNothing()),

    /**
     * 立即执行一次
     */
    FIRE_ONCE_NOW("立即执行一次", new MisfireFireOnceNow());

    private final String title;
    private final MisfireHandler misfireHandler;

    MisfireStrategyEnum(String title, MisfireHandler misfireHandler) {
        this.title = title;
        this.misfireHandler = misfireHandler;
    }

    public String getTitle() {
        return title;
    }

    public MisfireHandler getMisfireHandler() {
        return misfireHandler;
    }

    /**
     * 按名称匹配
     *
     * @param name          名称
     * @param defaultItem   默认值
     * @return 匹配的枚举项
     */
    public static MisfireStrategyEnum match(String name, MisfireStrategyEnum defaultItem) {
        if (name != null) {
            for (MisfireStrategyEnum item : MisfireStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }

}
