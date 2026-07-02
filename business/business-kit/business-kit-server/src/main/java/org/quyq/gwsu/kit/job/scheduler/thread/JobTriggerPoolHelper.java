package org.quyq.gwsu.kit.job.scheduler.thread;

import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.quyq.gwsu.kit.job.scheduler.trigger.TriggerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务触发线程池助手
 */
public class JobTriggerPoolHelper {
    private static final Logger logger = LoggerFactory.getLogger(JobTriggerPoolHelper.class);

    // 快/慢线程池
    private ThreadPoolExecutor fastTriggerPool = null;
    private ThreadPoolExecutor slowTriggerPool = null;

    /**
     * 启动
     */
    public void start() {
        fastTriggerPool = new ThreadPoolExecutor(
                10,
                JobAdminBootstrap.getInstance().getTriggerPoolFastMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                r -> new Thread(r, "kit-job, admin JobTriggerPoolHelper-fastTriggerPool-" + r.hashCode()),
                (r, executor) -> logger.error(">>>>>>>>>>> kit-job, admin JobTriggerPoolHelper-fastTriggerPool execute too fast, Runnable={}", r)
        );

        slowTriggerPool = new ThreadPoolExecutor(
                10,
                JobAdminBootstrap.getInstance().getTriggerPoolSlowMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(5000),
                r -> new Thread(r, "kit-job, admin JobTriggerPoolHelper-slowTriggerPool-" + r.hashCode()),
                (r, executor) -> logger.error(">>>>>>>>>>> kit-job, admin JobTriggerPoolHelper-slowTriggerPool execute too fast, Runnable={}", r)
        );
    }

    /**
     * 停止
     */
    public void stop() {
        fastTriggerPool.shutdownNow();
        slowTriggerPool.shutdownNow();
        logger.info(">>>>>>>>> kit-job trigger thread pool shutdown success.");
    }

    // 任务超时计数
    private volatile long minTim = System.currentTimeMillis() / 60000;
    private volatile ConcurrentMap<Integer, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();

    /**
     * 触发任务
     *
     * @param jobId                任务ID
     * @param triggerType          触发类型
     * @param failRetryCount       失败重试次数（>=0使用该值，<0使用任务配置值）
     * @param executorShardingParam 分片参数
     * @param executorParam        执行参数（null使用任务配置值）
     * @param addressList          执行器地址列表（null使用任务配置值）
     */
    public void trigger(final int jobId,
                        final TriggerTypeEnum triggerType,
                        final int failRetryCount,
                        final String executorShardingParam,
                        final String executorParam,
                        final String addressList) {

        // 选择线程池
        ThreadPoolExecutor triggerPool_ = fastTriggerPool;
        AtomicInteger jobTimeoutCount = jobTimeoutCountMap.get(jobId);
        if (jobTimeoutCount != null && jobTimeoutCount.get() > 10) {
            triggerPool_ = slowTriggerPool;
        }

        // 触发
        triggerPool_.execute(() -> {
            long start = System.currentTimeMillis();

            try {
                // 执行触发
                JobAdminBootstrap.getInstance().getJobTrigger().trigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            } finally {

                // 检查超时计数
                long minTim_now = System.currentTimeMillis() / 60000;
                if (minTim != minTim_now) {
                    minTim = minTim_now;
                    jobTimeoutCountMap.clear();
                }

                // 增加超时计数
                long cost = System.currentTimeMillis() - start;
                if (cost > 500) {
                    AtomicInteger timeoutCount = jobTimeoutCountMap.putIfAbsent(jobId, new AtomicInteger(1));
                    if (timeoutCount != null) {
                        timeoutCount.incrementAndGet();
                    }
                }
            }
        });
    }

}
