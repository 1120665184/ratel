package org.quyq.gwsu.kit.job.scheduler.openapi.impl;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.quyq.gwsu.kit.job.scheduler.trigger.TriggerTypeEnum;
import org.springframework.stereotype.Service;

/**
 * Admin任务业务实现（简化版，仅保留触发能力）
 */
@Service
public class AdminJobBizImpl {

    /**
     * 触发任务
     *
     * @param jobId         任务ID
     * @param executorParam 执行参数
     * @param addressList   地址列表
     * @return 响应
     */
    public R<String> triggerJob(int jobId, String executorParam, String addressList) {
        JobAdminBootstrap.getInstance().getJobTriggerPoolHelper().trigger(jobId, TriggerTypeEnum.API, -1, null, executorParam, addressList);
        return R.ok();
    }

}
