package org.quyq.gwsu.kit.job.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.kit.job.domain.KitJobInfo;

import java.util.List;

/**
 * 任务信息服务接口
 */
public interface IKitJobInfoService extends IService<KitJobInfo> {

    /**
     * 查询待调度的任务（trigger_status = 1 且 trigger_next_time <= maxNextTime）
     *
     * @param maxNextTime 最大下次触发时间
     * @param pagesize   每页数量
     * @return 待调度任务列表
     */
    List<KitJobInfo> scheduleJobQuery(long maxNextTime, int pagesize);

    /**
     * 批量更新调度信息（CASE WHEN，标准SQL保留在XML中）
     *
     * @param jobInfoList 任务列表
     * @return 影响行数
     */
    int scheduleBatchUpdate(List<KitJobInfo> jobInfoList);

}
