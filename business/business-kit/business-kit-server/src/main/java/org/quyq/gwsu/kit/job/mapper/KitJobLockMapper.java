package org.quyq.gwsu.kit.job.mapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 任务锁Mapper
 */
@Mapper
public interface KitJobLockMapper {

    /**
     * 获取调度锁
     */
    String scheduleLock();

}
