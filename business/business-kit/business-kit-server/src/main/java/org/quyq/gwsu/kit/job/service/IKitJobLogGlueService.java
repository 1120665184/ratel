package org.quyq.gwsu.kit.job.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.kit.job.domain.KitJobLogGlue;

/**
 * 任务日志Glue服务接口
 */
public interface IKitJobLogGlueService extends IService<KitJobLogGlue> {

    /**
     * 删除旧的Glue记录（保留最近limit条）
     *
     * @param jobId 任务ID
     * @param limit 保留条数
     */
    void removeOld(String jobId, int limit);

}
