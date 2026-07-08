package org.quyq.gwsu.common.job.config;

import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.quyq.gwsu.common.job.executor.XxlJobExecutor;
import org.quyq.gwsu.common.job.openapi.admin.JobAdminClientApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * 任务执行器自动配置
 * <p>
 * 所有参数从Spring属性自动推导：
 * - appname = spring.application.name
 * - logPath = {user.dir}/logs/{appName}/job
 * - logRetentionDays = 30
 * - glueEnabled = true
 */
@AutoConfiguration
@ConditionalOnProperty(name = CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".executor.enabled", havingValue = "true", matchIfMissing = true)
public class XxlJobAutoConfiguration {

    @Value("#{'${"+CoreConstants.Yaml.PROJECT_CONFIG_PREFIX+".job.executor.glue-enabled:false}'}")
    private boolean glueEnabled;

    @Bean
    public XxlJobExecutor xxlJobExecutor(JobAdminClientApi jobAdminClientApi , ProjectUtils projectUtils) {

        // 从Spring属性自动推导
        String applicationName = projectUtils.getApplicationName();
        String appname = StringUtils.hasText(applicationName) ? applicationName + "-executor": "default-job-executor";
        String logPath = System.getProperty("user.dir") + "/logs/" + applicationName + "/job";
        int logRetentionDays = 30;
        boolean glueEnabled = this.glueEnabled;

        return new XxlJobExecutor(appname, logPath, logRetentionDays, glueEnabled, jobAdminClientApi);
    }

}
