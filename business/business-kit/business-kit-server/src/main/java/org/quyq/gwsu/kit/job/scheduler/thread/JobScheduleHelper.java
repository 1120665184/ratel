package org.quyq.gwsu.kit.job.scheduler.thread;

import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.scheduler.constant.TriggerStatus;
import org.quyq.gwsu.kit.job.scheduler.misfire.MisfireStrategyEnum;
import org.quyq.gwsu.kit.job.scheduler.trigger.TriggerTypeEnum;
import org.quyq.gwsu.kit.job.scheduler.type.ScheduleTypeEnum;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 任务调度助手（时间轮 + 数据库轮询）
 */
public class JobScheduleHelper {
    private static final Logger logger = LoggerFactory.getLogger(JobScheduleHelper.class);

    /**
     * 预读取时间，提升效率
     */
    public static final long PRE_READ_MS = 5000;

    private static final long ELEGANT_SHUTDOWN_WAITING_SECONDS = 10;

    private Thread scheduleThread;
    private Thread ringThread;
    private volatile boolean scheduleThreadToStop = false;
    private volatile boolean ringThreadToStop = false;
    private final Map<Integer, List<String>> ringData = new ConcurrentHashMap<>();

    /**
     * 启动
     */
    public void start() {

        scheduleThreadToStop = false;
        ringThreadToStop = false;

        // 1、调度线程
        scheduleThread = new Thread(() -> {

            // 对齐时间
            try {
                TimeUnit.MILLISECONDS.sleep(5000 - System.currentTimeMillis() % 1000);
            } catch (Throwable e) {
                if (!scheduleThreadToStop) {
                    logger.error(e.getMessage(), e);
                }
            }
            logger.info(">>>>>>>>> kit-job admin scheduler 启动成功。");

            // 预读取数量
            int preReadCount = (JobAdminBootstrap.getInstance().getTriggerPoolFastMax() + JobAdminBootstrap.getInstance().getTriggerPoolSlowMax()) * 10;

            while (!scheduleThreadToStop) {

                long start = System.currentTimeMillis();
                boolean preReadSuc = true;

                TransactionStatus transactionStatus = null;
                try {
                    transactionStatus = JobAdminBootstrap.getInstance().getTransactionManager().getTransaction(new DefaultTransactionDefinition());

                    // 1、获取调度锁
                    JobAdminBootstrap.getInstance().getKitJobLockMapper().scheduleLock();
                    long nowTime = System.currentTimeMillis();

                    // 查询待调度任务
                    List<KitJobInfo> scheduleList = JobAdminBootstrap.getInstance().getKitJobInfoService().scheduleJobQuery(nowTime + PRE_READ_MS, preReadCount);
                    if (scheduleList != null && !scheduleList.isEmpty()) {

                        // 2、推入时间轮
                        for (KitJobInfo jobInfo : scheduleList) {

                            if (nowTime > jobInfo.getTriggerNextTime() + PRE_READ_MS) {
                                // 2.1、过期超过5s：忽略 && 生成下次触发时间

                                MisfireStrategyEnum misfireStrategyEnum = MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), MisfireStrategyEnum.DO_NOTHING);
                                misfireStrategyEnum.getMisfireHandler().handle(jobInfo.getId());

                                refreshNextTriggerTime(jobInfo, LocalDateTime.now());

                            } else if (nowTime >= jobInfo.getTriggerNextTime()) {
                                // 2.2、过期不超过5s：直接触发 && 生成下次触发时间

                                JobAdminBootstrap.getInstance().getJobTriggerPoolHelper().trigger(jobInfo.getId(), TriggerTypeEnum.CRON, -1, null, null, null);
                                logger.debug(">>>>>>>>>>> kit-job, 调度过期直接触发：jobId = {}", jobInfo.getId());

                                refreshNextTriggerTime(jobInfo, LocalDateTime.now());

                                // 下次触发在5s内，再次预读
                                if (jobInfo.getTriggerStatus() == TriggerStatus.RUNNING.getValue() && nowTime + PRE_READ_MS > jobInfo.getTriggerNextTime()) {
                                    int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);
                                    pushTimeRing(ringSecond, jobInfo.getId());
                                    logger.debug(">>>>>>>>>>> kit-job, 调度预读推入时间轮：jobId = {}", jobInfo.getId());
                                    refreshNextTriggerTime(jobInfo, LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(jobInfo.getTriggerNextTime()), ZoneId.systemDefault()));
                                }

                            } else {
                                // 2.3、预读：推入时间轮 && 生成下次触发时间

                                int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);
                                pushTimeRing(ringSecond, jobInfo.getId());
                                logger.debug(">>>>>>>>>>> kit-job, 调度正常推入时间轮：jobId = {}", jobInfo.getId());
                                refreshNextTriggerTime(jobInfo, LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(jobInfo.getTriggerNextTime()), ZoneId.systemDefault()));
                            }
                        }

                        // 3、批量更新调度信息
                        int batchSize = JobAdminBootstrap.getInstance().getScheduleBatchSize();
                        List<List<KitJobInfo>> scheduleListBatches = splitList(scheduleList, batchSize);
                        for (List<KitJobInfo> scheduleListBatch : scheduleListBatches) {
                            int totalAffected = JobAdminBootstrap.getInstance().getKitJobInfoService().scheduleBatchUpdate(scheduleListBatch);
                            logger.debug(">>>>>>>>>>> kit-job, JobScheduleHelper scheduleBatchUpdate records:{}", totalAffected);
                        }

                    } else {
                        preReadSuc = false;
                    }

                } catch (Throwable e) {
                    if (!scheduleThreadToStop) {
                        logger.error(">>>>>>>>>>> kit-job, JobScheduleHelper#scheduleThread error:{}", e.getMessage(), e);
                    }
                } finally {
                    try {
                        if (transactionStatus != null) {
                            JobAdminBootstrap.getInstance().getTransactionManager().commit(transactionStatus);
                        }
                    } catch (Throwable e) {
                        logger.error(">>>>>>>>>>> kit-job, JobScheduleHelper#scheduleThread transaction commit error:{}", e.getMessage(), e);
                    }
                }

                long cost = System.currentTimeMillis() - start;

                // 等待对齐秒
                if (cost < 1000) {
                    try {
                        TimeUnit.MILLISECONDS.sleep((preReadSuc ? 1000 : PRE_READ_MS) - System.currentTimeMillis() % 1000);
                    } catch (Throwable e) {
                        if (!scheduleThreadToStop) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }

            logger.info(">>>>>>>>>>> kit-job, JobScheduleHelper#scheduleThread 停止");
        });
        scheduleThread.setDaemon(true);
        scheduleThread.setName("kit-job, admin JobScheduleHelper#scheduleThread");
        scheduleThread.start();

        // 2、时间轮线程
        ringThread = new Thread(() -> {

            while (!ringThreadToStop) {

                // 对齐秒
                try {
                    TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                } catch (Throwable e) {
                    if (!ringThreadToStop) {
                        logger.error(e.getMessage(), e);
                    }
                }

                try {
                    List<String> ringItemData = new ArrayList<>();

                    int nowSecond = Calendar.getInstance().get(Calendar.SECOND);
                    for (int i = 0; i <= 2; i++) {
                        List<String> ringItemList = ringData.remove((nowSecond + 60 - i) % 60);
                        if (ringItemList != null && !ringItemList.isEmpty()) {
                            List<String> ringItemListDistinct = ringItemList.stream().distinct().toList();
                            if (ringItemListDistinct.size() < ringItemList.size()) {
                                logger.warn(">>>>>>>>>>> kit-job, 时间轮发现重复任务：{} = {}", nowSecond, ringItemData);
                            }
                            ringItemData.addAll(ringItemListDistinct);
                        }
                    }

                    logger.debug(">>>>>>>>>>> kit-job, 时间轮刻度：{} = {}", nowSecond, ringItemData);
                    if (!ringItemData.isEmpty()) {
                        for (String jobId : ringItemData) {
                            JobAdminBootstrap.getInstance().getJobTriggerPoolHelper().trigger(jobId, TriggerTypeEnum.CRON, -1, null, null, null);
                        }
                        ringItemData.clear();
                    }
                } catch (Throwable e) {
                    if (!ringThreadToStop) {
                        logger.error(">>>>>>>>>>> kit-job, JobScheduleHelper#ringThread error:{}", e.getMessage(), e);
                    }
                }
            }
            logger.info(">>>>>>>>>>> kit-job, JobScheduleHelper#ringThread 停止");
        });
        ringThread.setDaemon(true);
        ringThread.setName("kit-job, admin JobScheduleHelper#ringThread");
        ringThread.start();
    }

    /**
     * 刷新下次触发时间
     */
    private void refreshNextTriggerTime(KitJobInfo jobInfo, LocalDateTime fromTime) {
        try {
            ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
            LocalDateTime nextTriggerTime = scheduleTypeEnum.getScheduleType().generateNextTriggerTime(jobInfo, fromTime);

            if (nextTriggerTime != null) {
                jobInfo.setTriggerStatus(-1);
                jobInfo.setTriggerLastTime(jobInfo.getTriggerNextTime());
                jobInfo.setTriggerNextTime(nextTriggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            } else {
                jobInfo.setTriggerStatus(TriggerStatus.STOPPED.getValue());
                jobInfo.setTriggerLastTime(0);
                jobInfo.setTriggerNextTime(0);
                logger.error(">>>>>>>>>>> kit-job, refreshNextValidTime fail for job: jobId={}, scheduleType={}, scheduleConf={}",
                        jobInfo.getId(), jobInfo.getScheduleType(), jobInfo.getScheduleConf());
            }
        } catch (Throwable e) {
            jobInfo.setTriggerStatus(TriggerStatus.STOPPED.getValue());
            jobInfo.setTriggerLastTime(0);
            jobInfo.setTriggerNextTime(0);
            logger.error(">>>>>>>>>>> kit-job, refreshNextValidTime error for job: jobId={}, scheduleType={}, scheduleConf={}",
                    jobInfo.getId(), jobInfo.getScheduleType(), jobInfo.getScheduleConf(), e);
        }
    }

    /**
     * 推入时间轮
     */
    private void pushTimeRing(int ringSecond, String jobId) {
        List<String> ringItemList = ringData.computeIfAbsent(ringSecond, k -> new ArrayList<>());
        ringItemList.add(jobId);
        logger.debug(">>>>>>>>>>> kit-job, 推入时间轮：{} = {}", ringSecond, ringItemList);
    }

    /**
     * 停止
     */
    public void stop() {

        // 1、停止调度线程
        scheduleThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        if (scheduleThread.getState() != Thread.State.TERMINATED) {
            scheduleThread.interrupt();
            try {
                scheduleThread.join();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }

        // 优雅关闭：等待时间轮数据
        boolean hasRingData = false;
        if (!ringData.isEmpty()) {
            for (int second : ringData.keySet()) {
                List<String> ringItemList = ringData.get(second);
                if (ringItemList != null && !ringItemList.isEmpty()) {
                    hasRingData = true;
                    break;
                }
            }
        }
        if (hasRingData) {
            try {
                TimeUnit.SECONDS.sleep(ELEGANT_SHUTDOWN_WAITING_SECONDS);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }

        // 2、停止时间轮线程
        ringThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        if (ringThread.getState() != Thread.State.TERMINATED) {
            ringThread.interrupt();
            try {
                ringThread.join();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }

        logger.info(">>>>>>>>>>> kit-job, JobScheduleHelper 停止");
    }

    /**
     * 分割列表
     */
    private <T> List<List<T>> splitList(List<T> list, int batchSize) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            result.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return result;
    }

}
