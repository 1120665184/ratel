package org.quyq.gwsu.common.job.thread;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.job.constant.JobConst;
import org.quyq.gwsu.common.job.constant.RegistTypeEnum;
import org.quyq.gwsu.common.job.executor.XxlJobExecutor;
import org.quyq.gwsu.common.job.openapi.admin.dto.RegistryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 执行器注册辅助类
 * <p>
 * 单体模式不注册；分布式模式通过JobAdminClientApi注册。
 * 使用ScheduledExecutorService替代CyclicThread
 */
public class ExecutorRegistryHelper {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorRegistryHelper.class);

    /**
     * 注册线程
     */
    private ScheduledExecutorService registryScheduler;

    /**
     * 注册地址
     */
    private String registryAddress;

    /**
     * 启动
     */
    public void start(final XxlJobExecutor xxlJobExecutor) {

        // 构建注册地址
        if (DeployUtils.isSingle()) {
            // 单体模式：Admin和Executor在同一JVM，使用本地标识
            registryAddress = "local";
        } else {
            // 分布式模式：从Nacos获取IP和端口
            try {
                Environment environment = SpringUtils.getBean(Environment.class);
                String ip = environment.getProperty("spring.cloud.client.ip-address");
                String port = environment.getProperty("server.port", "8080");

                if (ip == null || ip.isEmpty()) {
                    logger.warn(">>>>>>>>>>> xxl-job executor registry config fail, ip-address is null.");
                    return;
                }

                registryAddress = "http://" + ip + ":" + port + "/";
            } catch (Exception e) {
                logger.warn(">>>>>>>>>>> xxl-job executor registry config fail, cannot get ip-address.", e);
                return;
            }
        }

        // 注册线程
        registryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ExecutorRegistryHelper#registryThread");
            t.setDaemon(true);
            return t;
        });

        registryScheduler.scheduleAtFixedRate(() -> {
            try {
                RegistryRequest registryParam = new RegistryRequest(
                        RegistTypeEnum.EXECUTOR.name(),
                        xxlJobExecutor.getAppname(),
                        registryAddress
                );
                R<String> registryResult = xxlJobExecutor.getJobAdminClientApi().registry(registryParam);
                if (registryResult != null && registryResult.isSuccess()) {
                    logger.debug(">>>>>>>>>>> xxl-job registry success, registryParam:{}, registryResult:{}", registryParam, registryResult);
                } else {
                    logger.info(">>>>>>>>>>> xxl-job registry fail, registryParam:{}, registryResult:{}", registryParam, registryResult);
                }
            } catch (Throwable e) {
                logger.info(">>>>>>>>>>> xxl-job registry error, registryParam:{}", e.getMessage(), e);
            }
        }, 0, JobConst.REGISTRY_BEAT_INTERVAL, TimeUnit.SECONDS);

        logger.info(">>>>>>>>>>> xxl-job executor registry start, appname:{}, address:{}", xxlJobExecutor.getAppname(), registryAddress);
    }

    /**
     * 停止
     */
    public void stop(final XxlJobExecutor xxlJobExecutor) {

        // 1、停止注册线程
        if (registryScheduler != null) {
            registryScheduler.shutdown();
        }

        // 2、注销注册
        registryRemove(xxlJobExecutor);
    }

    private void registryRemove(final XxlJobExecutor xxlJobExecutor) {
        if (registryAddress == null) {
            return;
        }

        RegistryRequest registryParam = new RegistryRequest(
                RegistTypeEnum.EXECUTOR.name(),
                xxlJobExecutor.getAppname(),
                registryAddress
        );
        try {
            R<String> registryResult = xxlJobExecutor.getJobAdminClientApi().registryRemove(registryParam);
            if (registryResult != null && registryResult.isSuccess()) {
                logger.info(">>>>>>>>>>> xxl-job registry-remove success, registryParam:{}, registryResult:{}", registryParam, registryResult);
            } else {
                logger.info(">>>>>>>>>>> xxl-job registry-remove fail, registryParam:{}, registryResult:{}", registryParam, registryResult);
            }
        } catch (Throwable e) {
            logger.warn(">>>>>>>>>>> xxl-job registry-remove error, registryParam:{}, error:{}", registryParam, e.getMessage());
        }
    }

}
