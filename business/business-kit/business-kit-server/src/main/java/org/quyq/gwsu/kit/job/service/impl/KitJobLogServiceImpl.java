package org.quyq.gwsu.kit.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.job.domain.KitJobLog;
import org.quyq.gwsu.kit.job.mapper.KitJobLogMapper;
import org.quyq.gwsu.kit.job.service.IKitJobLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务日志服务实现
 */
@Service
@RequiredArgsConstructor
public class KitJobLogServiceImpl extends ServiceImpl<KitJobLogMapper, KitJobLog> implements IKitJobLogService {

    @Override
    public Map<String, Object> findLogReport(LocalDateTime from, LocalDateTime to) {
        // 查询时间范围内的日志的 triggerCode 和 handleCode
        LambdaQueryWrapper<KitJobLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(KitJobLog::getTriggerCode, KitJobLog::getHandleCode)
                .between(KitJobLog::getTriggerTime, from, to);

        List<KitJobLog> logs = this.list(wrapper);

        int triggerDayCount = logs.size();
        int triggerDayCountRunning = (int) logs.stream()
                .filter(log -> (log.getTriggerCode() == 0 || log.getTriggerCode() == 200) && log.getHandleCode() == 0)
                .count();
        int triggerDayCountSuc = (int) logs.stream()
                .filter(log -> log.getHandleCode() == 200)
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("triggerDayCount", triggerDayCount);
        result.put("triggerDayCountRunning", triggerDayCountRunning);
        result.put("triggerDayCountSuc", triggerDayCountSuc);
        return result;
    }

    @Override
    public List<String> findClearLogIds(String jobId, LocalDateTime clearBeforeTime, int clearBeforeNum, int pagesize) {
        // 1. 如果 clearBeforeNum > 0，先查出该 jobId 最近的 N 条日志 ID（保留列表）
        Set<String> retainIds = Collections.emptySet();
        if (clearBeforeNum > 0) {
            LambdaQueryWrapper<KitJobLog> retainWrapper = new LambdaQueryWrapper<>();
            retainWrapper.select(KitJobLog::getId)
                    .eq(jobId != null && !jobId.isEmpty(), KitJobLog::getJobId, jobId)
                    .orderByDesc(KitJobLog::getTriggerTime)
                    .last("LIMIT " + clearBeforeNum);

            List<KitJobLog> retainLogs = this.list(retainWrapper);
            retainIds = retainLogs.stream().map(KitJobLog::getId).collect(Collectors.toSet());
        }

        // 2. 查询待清理的日志 ID
        LambdaQueryWrapper<KitJobLog> clearWrapper = new LambdaQueryWrapper<>();
        clearWrapper.select(KitJobLog::getId)
                .eq(jobId != null && !jobId.isEmpty(), KitJobLog::getJobId, jobId)
                .le(clearBeforeTime != null, KitJobLog::getTriggerTime, clearBeforeTime)
                .notIn(!retainIds.isEmpty(), KitJobLog::getId, retainIds)
                .orderByAsc(KitJobLog::getId)
                .last("LIMIT " + pagesize);

        List<KitJobLog> clearLogs = this.list(clearWrapper);
        return clearLogs.stream().map(KitJobLog::getId).toList();
    }

    @Override
    public List<String> findFailJobLogIds(int pagesize) {
        // 失败条件：NOT ((trigger_code IN (0,200) AND handle_code = 0) OR (handle_code = 200))
        // 即：(trigger_code NOT IN (0,200) OR (handle_code != 0 AND handle_code != 200))
        LambdaQueryWrapper<KitJobLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(KitJobLog::getId)
                .eq(KitJobLog::getAlarmStatus, 0)
                .and(w -> w
                        .notIn(KitJobLog::getTriggerCode, 0, 200)
                        .or()
                        .nested(n -> n.ne(KitJobLog::getHandleCode, 0).ne(KitJobLog::getHandleCode, 200))
                )
                .orderByAsc(KitJobLog::getId)
                .last("LIMIT " + pagesize);

        List<KitJobLog> logs = this.list(wrapper);
        return logs.stream().map(KitJobLog::getId).toList();
    }

    @Override
    public int updateTriggerInfo(KitJobLog kitJobLog) {
        return baseMapper.updateTriggerInfo(kitJobLog);
    }

    @Override
    public int updateHandleInfo(KitJobLog kitJobLog) {
        return baseMapper.updateHandleInfo(kitJobLog);
    }

    @Override
    public int clearLog(List<String> logIds) {
        return baseMapper.clearLog(logIds);
    }

    @Override
    public int updateAlarmStatus(String logId, int oldAlarmStatus, int newAlarmStatus) {
        return baseMapper.updateAlarmStatus(logId, oldAlarmStatus, newAlarmStatus);
    }

    @Override
    public List<String> findLostJobIds(LocalDateTime losedTime) {
        return baseMapper.findLostJobIds(losedTime);
    }

}
