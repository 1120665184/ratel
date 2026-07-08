package org.quyq.gwsu.kit.config.properties;


import lombok.Data;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 任务调度管理端配置属性
 * <p>
 * 对应配置前缀：org.quyq.job.admin
 *
 * @author Quyq
 * @date 2026/7/7
 */
@ConfigurationProperties(CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".job.admin")
@Data
public class JobAdminProperties {

    /**
     * 快触发线程池最大线程数
     * <p>
     * 用于处理正常优先级的任务触发，最低200
     */
    private int triggerPoolFastMax = 200;

    /**
     * 慢触发线程池最大线程数
     * <p>
     * 用于处理慢任务（执行时间超过10s）的触发，最低100
     */
    private int triggerPoolSlowMax = 100;

    /**
     * 调度批量查询大小
     * <p>
     * 每次调度从数据库读取的任务数量，范围50~500，超出范围默认100
     */
    private int scheduleBatchSize = 100;

    /**
     * 日志保留天数
     * <p>
     * 超过该天数的日志将被自动清理，最低3天，低于3则关闭自动清理（返回-1）
     */
    private int logretentiondays = 30;

    /**
     * 任务超时时间（秒）
     * <p>
     * 任务执行超过该时间将被标记为超时，0表示不限制
     */
    private int timeout = 3;

}
