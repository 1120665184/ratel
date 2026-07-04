package org.quyq.gwsu.kit.job.scheduler.config;


import jakarta.annotation.Resource;
import org.quyq.gwsu.kit.job.mapper.*;
import org.quyq.gwsu.kit.job.scheduler.alarm.JobAlarmer;
import org.quyq.gwsu.kit.job.scheduler.complete.JobCompleter;
import org.quyq.gwsu.kit.job.scheduler.thread.*;
import org.quyq.gwsu.kit.job.scheduler.trigger.JobTrigger;
import org.quyq.gwsu.kit.job.scheduler.trigger.TriggerStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;

/**
 * 任务调度管理端启动类
 */
@Component
public class JobAdminBootstrap implements InitializingBean, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(JobAdminBootstrap.class);

    // ---------------------- 单例 ----------------------

    private static JobAdminBootstrap adminConfig = null;

    public static JobAdminBootstrap getInstance() {
        return adminConfig;
    }

    // ---------------------- 启动/停止 ----------------------

    @Override
    public void afterPropertiesSet() throws Exception {
        adminConfig = this;
        doStart();
    }

    @Override
    public void destroy() throws Exception {
        doStop();
    }

    // 调度模块
    private JobTriggerPoolHelper jobTriggerPoolHelper;
    private JobRegistryHelper jobRegistryHelper;
    private JobFailAlarmMonitorHelper jobFailAlarmMonitorHelper;
    private JobCompleteHelper jobCompleteHelper;
    private JobLogReportHelper jobLogReportHelper;
    private JobScheduleHelper jobScheduleHelper;

    public JobTriggerPoolHelper getJobTriggerPoolHelper() {
        return jobTriggerPoolHelper;
    }

    public JobRegistryHelper getJobRegistryHelper() {
        return jobRegistryHelper;
    }

    public JobCompleteHelper getJobCompleteHelper() {
        return jobCompleteHelper;
    }

    /**
     * 启动
     */
    private void doStart() throws Exception {
        // 触发线程池
        jobTriggerPoolHelper = new JobTriggerPoolHelper();
        jobTriggerPoolHelper.start();

        // 注册监控
        jobRegistryHelper = new JobRegistryHelper();
        jobRegistryHelper.start();

        // 失败告警监控
        jobFailAlarmMonitorHelper = new JobFailAlarmMonitorHelper();
        jobFailAlarmMonitorHelper.start();

        // 任务完成（依赖JobTriggerPoolHelper）
        jobCompleteHelper = new JobCompleteHelper();
        jobCompleteHelper.start();

        // 日志报表
        jobLogReportHelper = new JobLogReportHelper();
        jobLogReportHelper.start();

        // 任务调度（依赖JobTriggerPoolHelper）
        jobScheduleHelper = new JobScheduleHelper();
        jobScheduleHelper.start();

        logger.info(">>>>>>>>> kit-job admin 启动成功。");
    }

    /**
     * 停止
     */
    private void doStop() {
        // 任务调度停止
        jobScheduleHelper.stop();

        // 日志报表停止
        jobLogReportHelper.stop();

        // 任务完成停止
        jobCompleteHelper.stop();

        // 失败告警监控停止
        jobFailAlarmMonitorHelper.stop();

        // 注册监控停止
        jobRegistryHelper.stop();

        // 触发线程池停止
        jobTriggerPoolHelper.stop();

        logger.info(">>>>>>>>> kit-job admin 已停止。");
    }

    // ---------------------- 配置字段 ----------------------

    @Value("${xxl.job.triggerpool.fast.max:200}")
    private int triggerPoolFastMax;

    @Value("${xxl.job.triggerpool.slow.max:100}")
    private int triggerPoolSlowMax;

    @Value("${xxl.job.schedule.batchsize:100}")
    private int scheduleBatchSize;

    @Value("${xxl.job.logretentiondays:30}")
    private int logretentiondays;

    @Value("${xxl.job.timeout:3}")
    private int timeout;

    // ---------------------- 依赖注入 ----------------------

    @Resource
    private KitJobLogMapper kitJobLogMapper;
    @Resource
    private KitJobInfoMapper kitJobInfoMapper;
    @Resource
    private KitJobRegistryMapper kitJobRegistryMapper;
    @Resource
    private KitJobGroupMapper kitJobGroupMapper;
    @Resource
    private KitJobLogReportMapper kitJobLogReportMapper;
    @Resource
    private KitJobLockMapper kitJobLockMapper;
    @Resource
    private PlatformTransactionManager transactionManager;
    @Resource
    private JobAlarmer jobAlarmer;
    @Resource
    private JobTrigger jobTrigger;
    @Resource
    private JobCompleter jobCompleter;
    @Resource
    private TriggerStrategy triggerStrategy;
    @Resource
    private ObjectMapper objectMapper;

    // ---------------------- Getter ----------------------

    public int getTriggerPoolFastMax() {
        if (triggerPoolFastMax < 200) {
            return 200;
        }
        return triggerPoolFastMax;
    }

    public int getTriggerPoolSlowMax() {
        if (triggerPoolSlowMax < 100) {
            return 100;
        }
        return triggerPoolSlowMax;
    }

    public int getScheduleBatchSize() {
        if (!(scheduleBatchSize >= 50 && scheduleBatchSize <= 500)) {
            return 100;
        }
        return scheduleBatchSize;
    }

    public int getLogretentiondays() {
        if (logretentiondays < 3) {
            return -1;  // 限制大于等于3，否则关闭
        }
        return logretentiondays;
    }

    public int getTimeout() {
        return timeout;
    }

    public KitJobLogMapper getKitJobLogMapper() {
        return kitJobLogMapper;
    }

    public KitJobInfoMapper getKitJobInfoMapper() {
        return kitJobInfoMapper;
    }

    public KitJobRegistryMapper getKitJobRegistryMapper() {
        return kitJobRegistryMapper;
    }

    public KitJobGroupMapper getKitJobGroupMapper() {
        return kitJobGroupMapper;
    }

    public KitJobLogReportMapper getKitJobLogReportMapper() {
        return kitJobLogReportMapper;
    }

    public KitJobLockMapper getKitJobLockMapper() {
        return kitJobLockMapper;
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public JobAlarmer getJobAlarmer() {
        return jobAlarmer;
    }

    public JobTrigger getJobTrigger() {
        return jobTrigger;
    }

    public JobCompleter getJobCompleter() {
        return jobCompleter;
    }

    public TriggerStrategy getTriggerStrategy() {
        return triggerStrategy;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

}
