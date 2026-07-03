package org.quyq.gwsu.kit.job.scheduler.complete;

import jakarta.annotation.Resource;
import org.quyq.gwsu.common.job.constant.JobConst;
import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.domain.KitJobLog;
import org.quyq.gwsu.kit.job.mapper.KitJobInfoMapper;
import org.quyq.gwsu.kit.job.mapper.KitJobLogMapper;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.quyq.gwsu.kit.job.scheduler.trigger.TriggerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

/**
 * 任务完成处理器
 */
@Component
public class JobCompleter {
    private static final Logger logger = LoggerFactory.getLogger(JobCompleter.class);

    @Resource
    private KitJobInfoMapper kitJobInfoMapper;
    @Resource
    private KitJobLogMapper kitJobLogMapper;

    /**
     * 完成任务（仅处理一次）
     */
    public int complete(KitJobLog kitJobLog) {

        // 1、处理子任务
        processChildJob(kitJobLog);

        // text最大64kb 避免长度过长
        if (kitJobLog.getHandleMsg() != null && kitJobLog.getHandleMsg().length() > 15000) {
            kitJobLog.setHandleMsg(kitJobLog.getHandleMsg().substring(0, 15000));
        }

        // 2、更新任务处理信息
        return kitJobLogMapper.updateHandleInfo(kitJobLog);
    }

    /**
     * 处理子任务
     */
    private void processChildJob(KitJobLog kitJobLog) {

        // 1、处理成功时，触发子任务
        String triggerChildMsg = null;
        if (JobConst.HANDLE_CODE_SUCCESS == kitJobLog.getHandleCode()) {
            KitJobInfo kitJobInfo = kitJobInfoMapper.selectById(kitJobLog.getJobId());

            if (kitJobInfo != null && kitJobInfo.getChildJobId() != null && !kitJobInfo.getChildJobId().trim().isEmpty()) {
                triggerChildMsg = "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发子任务<<<<<<<<<<<<< </span><br>";
                String[] childJobIds = kitJobInfo.getChildJobId().split(",");
                for (int i = 0; i < childJobIds.length; i++) {

                    int childJobId = (childJobIds[i] != null && !childJobIds[i].trim().isEmpty()
                            && isNumeric(childJobIds[i].trim()))
                            ? Integer.parseInt(childJobIds[i].trim())
                            : -1;
                    if (childJobId > 0) {
                        // 校验：不能触发自身
                        if (childJobId == kitJobLog.getJobId()) {
                            logger.debug(">>>>>>>>>>> 任务完成处理器忽略子任务，childJobId {} 是自身。", childJobId);
                            continue;
                        }

                        // 触发子任务
                        JobAdminBootstrap.getInstance().getJobTriggerPoolHelper().trigger(childJobId, TriggerTypeEnum.PARENT, -1, null, null, null);

                        // 添加消息
                        triggerChildMsg += MessageFormat.format("子任务[{0}]触发{1}, 子任务ID:{2}",
                                (i + 1),
                                "成功",
                                childJobIds[i]);
                    } else {
                        triggerChildMsg += MessageFormat.format("子任务[{0}]触发失败, 子任务ID:{1}",
                                (i + 1),
                                childJobIds[i]);
                    }
                }
            }
        }

        // 2、追加子任务触发消息
        if (triggerChildMsg != null && !triggerChildMsg.isEmpty()) {
            kitJobLog.setHandleMsg(kitJobLog.getHandleMsg() + triggerChildMsg);
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
