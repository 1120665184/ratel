package org.quyq.gwsu.kit.job.scheduler.config;


import jakarta.annotation.Resource;
import org.quyq.gwsu.kit.config.properties.JobAdminProperties;
import org.quyq.gwsu.kit.job.mapper.KitJobLockMapper;
import org.quyq.gwsu.kit.job.scheduler.alarm.JobAlarmer;
import org.quyq.gwsu.kit.job.scheduler.complete.JobCompleter;
import org.quyq.gwsu.kit.job.scheduler.thread.*;
import org.quyq.gwsu.kit.job.scheduler.trigger.JobTrigger;
import org.quyq.gwsu.kit.job.scheduler.trigger.TriggerStrategy;
import org.quyq.gwsu.kit.job.service.IKitJobInfoService;
import org.quyq.gwsu.kit.job.service.IKitJobLogReportService;
import org.quyq.gwsu.kit.job.service.IKitJobLogService;
import org.quyq.gwsu.kit.job.service.IKitJobRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
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


    // ---------------------- 依赖注入 ----------------------

    @Resource
    private JobAdminProperties jobAdminProperties;

    @Resource
    private IKitJobLogService kitJobLogService;
    @Resource
    private IKitJobInfoService kitJobInfoService;
    @Resource
    private IKitJobRegistryService kitJobRegistryService;
    @Resource
    private IKitJobLogReportService kitJobLogReportService;
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
        if (jobAdminProperties.getTriggerPoolFastMax() < 200) {
            return 200;
        }
        return jobAdminProperties.getTriggerPoolFastMax();
    }

    public int getTriggerPoolSlowMax() {
        if (jobAdminProperties.getTriggerPoolSlowMax() < 100) {
            return 100;
        }
        return jobAdminProperties.getTriggerPoolSlowMax();
    }

    public int getScheduleBatchSize() {
        if (!(jobAdminProperties.getScheduleBatchSize() >= 50 && jobAdminProperties.getScheduleBatchSize() <= 500)) {
            return 100;
        }
        return jobAdminProperties.getScheduleBatchSize();
    }

    public int getLogretentiondays() {
        if (jobAdminProperties.getLogretentiondays() < 3) {
            return -1;  // 限制大于等于3，否则关闭
        }
        return jobAdminProperties.getLogretentiondays();
    }

    public int getTimeout() {
        return jobAdminProperties.getTimeout();
    }

    public IKitJobLogService getKitJobLogService() {
        return kitJobLogService;
    }

    public IKitJobInfoService getKitJobInfoService() {
        return kitJobInfoService;
    }

    public IKitJobRegistryService getKitJobRegistryService() {
        return kitJobRegistryService;
    }

    public IKitJobLogReportService getKitJobLogReportService() {
        return kitJobLogReportService;
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
