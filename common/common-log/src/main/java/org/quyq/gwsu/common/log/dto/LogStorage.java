package org.quyq.gwsu.common.log.dto;


import org.quyq.gwsu.common.log.enums.SaveMedium;

/**
 * @author Quyq
 * @date 2026/5/14
 * @description 日志存储相关配置
 */
public record LogStorage(
        /**
         * 数据存储媒介
         */
        SaveMedium medium ,

        /**
         * 数据生命周期配置
         */
        LogLifeCycle dataLifeCycle
) {
}
