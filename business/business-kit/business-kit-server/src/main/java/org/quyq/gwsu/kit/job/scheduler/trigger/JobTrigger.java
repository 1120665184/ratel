package org.quyq.gwsu.kit.job.scheduler.trigger;

import jakarta.annotation.Resource;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.constant.ExecutorBlockStrategyEnum;
import org.quyq.gwsu.common.job.constant.JobConst;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.quyq.gwsu.kit.job.domain.KitJobGroup;
import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.domain.KitJobLog;
import org.quyq.gwsu.kit.job.mapper.KitJobGroupMapper;
import org.quyq.gwsu.kit.job.mapper.KitJobInfoMapper;
import org.quyq.gwsu.kit.job.mapper.KitJobLogMapper;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.quyq.gwsu.kit.job.scheduler.route.ExecutorRouteStrategyEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 任务触发器
 */
@Component
public class JobTrigger {
    private static final Logger logger = LoggerFactory.getLogger(JobTrigger.class);

    @Resource
    private KitJobInfoMapper kitJobInfoMapper;
    @Resource
    private KitJobGroupMapper kitJobGroupMapper;
    @Resource
    private KitJobLogMapper kitJobLogMapper;

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
    public void trigger(String jobId,
                        TriggerTypeEnum triggerType,
                        int failRetryCount,
                        String executorShardingParam,
                        String executorParam,
                        String addressList) {

        // 加载任务数据
        KitJobInfo jobInfo = kitJobInfoMapper.selectById(jobId);
        if (jobInfo == null) {
            logger.warn(">>>>>>>>>>>> 触发失败，任务ID无效，jobId={}", jobId);
            return;
        }
        if (executorParam != null) {
            jobInfo.setExecutorParam(executorParam);
        }
        int finalFailRetryCount = failRetryCount >= 0 ? failRetryCount : jobInfo.getExecutorFailRetryCount();
        KitJobGroup group = kitJobGroupMapper.selectById(jobInfo.getJobGroup());

        // 覆盖地址列表
        if (addressList != null && !addressList.trim().isEmpty()) {
            group.setAddressType(1);
            group.setAddressList(addressList.trim());
        }

        // 分片参数
        int[] shardingParam = null;
        LocalDateTime triggerTime = LocalDateTime.now();
        if (executorShardingParam != null) {
            String[] shardingArr = executorShardingParam.split("/");
            if (shardingArr.length == 2 && isNumeric(shardingArr[0]) && isNumeric(shardingArr[1])) {
                shardingParam = new int[2];
                shardingParam[0] = Integer.parseInt(shardingArr[0]);
                shardingParam[1] = Integer.parseInt(shardingArr[1]);
            }
        }
        if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null)
                && group.getRegistryList() != null && !group.getRegistryList().isEmpty()
                && shardingParam == null) {
            for (int i = 0; i < group.getRegistryList().size(); i++) {
                processTrigger(group, jobInfo, finalFailRetryCount, triggerType, triggerTime, i, group.getRegistryList().size());
            }
        } else {
            if (shardingParam == null) {
                shardingParam = new int[]{0, 1};
            }
            processTrigger(group, jobInfo, finalFailRetryCount, triggerType, triggerTime, shardingParam[0], shardingParam[1]);
        }

    }

    /**
     * 处理触发
     */
    private void processTrigger(KitJobGroup group,
                                KitJobInfo jobInfo,
                                int finalFailRetryCount,
                                TriggerTypeEnum triggerType,
                                LocalDateTime triggerTime,
                                int index,
                                int total) {

        // 参数
        ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), ExecutorBlockStrategyEnum.SERIAL_EXECUTION);
        ExecutorRouteStrategyEnum executorRouteStrategyEnum = ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null);
        String shardingParam = (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) ? String.valueOf(index).concat("/").concat(String.valueOf(total)) : null;

        // 1、保存日志ID
        KitJobLog jobLog = new KitJobLog();
        jobLog.setJobGroup(jobInfo.getJobGroup());
        jobLog.setJobId(jobInfo.getId());
        jobLog.setTriggerTime(triggerTime);
        kitJobLogMapper.insert(jobLog);
        logger.debug(">>>>>>>>>>> kit-job 触发开始，jobId:{}", jobLog.getJobId());

        // 2、初始化触发参数
        TriggerRequest triggerParam = new TriggerRequest();
        triggerParam.setJobId(jobInfo.getId());
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        triggerParam.setExecutorParams(jobInfo.getExecutorParam());
        triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout());
        triggerParam.setLogId(jobLog.getId());
        triggerParam.setLogDateTime(jobLog.getTriggerTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        triggerParam.setGlueType(jobInfo.getGlueType());
        triggerParam.setGlueSource(jobInfo.getGlueSource());
        triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        triggerParam.setBroadcastIndex(index);
        triggerParam.setBroadcastTotal(total);

        // 3、初始化地址
        String address = null;
        R<String> routeAddressResult = null;
        if (group.getRegistryList() != null && !group.getRegistryList().isEmpty()) {
            if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) {
                if (index < group.getRegistryList().size()) {
                    address = group.getRegistryList().get(index);
                } else {
                    address = group.getRegistryList().get(0);
                }
            } else {
                routeAddressResult = executorRouteStrategyEnum.getRouter().route(triggerParam, group);
                if (routeAddressResult.isSuccess()) {
                    address = routeAddressResult.data();
                }
            }
        } else {
            routeAddressResult = R.fail("执行器注册地址为空");
        }

        // 4、触发远程执行器
        R<String> triggerResult;
        if (address != null) {
            triggerResult = doTrigger(triggerParam, address);
        } else {
            triggerResult = R.fail("地址路由失败");
        }

        // 5、收集触发信息
        StringBuilder triggerMsgSb = new StringBuilder();
        triggerMsgSb.append("触发类型：").append(triggerType.getTitle());
        try {
            triggerMsgSb.append("<br>调度机器：").append(InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            triggerMsgSb.append("<br>调度机器：获取失败");
        }
        triggerMsgSb.append("<br>执行器注册类型：").append(group.getAddressType() == 0 ? "自动注册" : "手动录入");
        triggerMsgSb.append("<br>执行器地址列表：").append(group.getRegistryList());
        triggerMsgSb.append("<br>路由策略：").append(executorRouteStrategyEnum.getTitle());
        if (shardingParam != null) {
            triggerMsgSb.append("(").append(shardingParam).append(")");
        }
        triggerMsgSb.append("<br>阻塞处理策略：").append(blockStrategy.getTitle());
        triggerMsgSb.append("<br>任务超时时间：").append(jobInfo.getExecutorTimeout());
        triggerMsgSb.append("<br>失败重试次数：").append(finalFailRetryCount);

        // 触发数据
        triggerMsgSb.append("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>");
        triggerMsgSb.append("<br>执行器地址：");
        if (address != null && !address.isEmpty()) {
            triggerMsgSb.append(address);
        } else if (routeAddressResult != null && !routeAddressResult.isSuccess() && routeAddressResult.msg() != null) {
            triggerMsgSb.append("地址路由失败，").append(routeAddressResult.msg());
        } else {
            triggerMsgSb.append("地址路由失败");
        }
        if (jobInfo.getExecutorHandler() != null && !jobInfo.getExecutorHandler().isEmpty()) {
            triggerMsgSb.append("<br>JobHandler：").append(jobInfo.getExecutorHandler());
        }
        triggerMsgSb.append("<br>任务参数：").append(jobInfo.getExecutorParam());
        triggerMsgSb.append("<br>触发结果：");
        if (triggerResult.isSuccess()) {
            triggerMsgSb.append("成功");
        } else if (triggerResult.msg() != null) {
            triggerMsgSb.append("失败，").append(triggerResult.msg());
        } else {
            triggerMsgSb.append("失败");
        }

        // 6、保存日志触发信息
        jobLog.setExecutorAddress(address);
        jobLog.setExecutorHandler(jobInfo.getExecutorHandler());
        jobLog.setExecutorParam(jobInfo.getExecutorParam());
        jobLog.setExecutorShardingParam(shardingParam);
        jobLog.setExecutorFailRetryCount(finalFailRetryCount);
        jobLog.setTriggerCode(triggerResult.isSuccess() ? JobConst.HANDLE_CODE_SUCCESS : JobConst.HANDLE_CODE_FAIL);
        jobLog.setTriggerMsg(triggerMsgSb.toString());
        kitJobLogMapper.updateTriggerInfo(jobLog);

        logger.debug(">>>>>>>>>>> kit-job 触发结束，jobId:{}", jobLog.getJobId());
    }

    /**
     * 执行触发
     */
    private R<String> doTrigger(TriggerRequest triggerParam, String address) {
        try {
            // 通过TriggerStrategy触发
            R<String> runResult = JobAdminBootstrap.getInstance().getTriggerStrategy().trigger(address, triggerParam);

            // 构建结果
            StringBuilder runResultSB = new StringBuilder("触发调度：");
            runResultSB.append("<br>address：").append(address);
            runResultSB.append("<br>code：").append(runResult.code());
            runResultSB.append("<br>msg：").append(runResult.msg());

            return R.ok(runResult.data(), runResultSB.toString());
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> kit-job 触发错误，请检查执行器[{}]是否运行。", address, e);
            return R.fail("触发异常：" + e.getMessage());
        }
    }

    private static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
