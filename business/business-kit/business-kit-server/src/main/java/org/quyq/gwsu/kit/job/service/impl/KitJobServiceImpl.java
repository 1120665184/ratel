package org.quyq.gwsu.kit.job.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.job.constant.ExecutorBlockStrategyEnum;
import org.quyq.gwsu.common.job.glue.GlueTypeEnum;
import org.quyq.gwsu.kit.job.domain.*;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.job.mapper.*;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.quyq.gwsu.kit.job.scheduler.constant.TriggerStatus;
import org.quyq.gwsu.kit.job.scheduler.cron.CronExpression;
import org.quyq.gwsu.kit.job.scheduler.misfire.MisfireStrategyEnum;
import org.quyq.gwsu.kit.job.scheduler.route.ExecutorRouteStrategyEnum;
import org.quyq.gwsu.kit.job.scheduler.thread.JobScheduleHelper;
import org.quyq.gwsu.kit.job.scheduler.trigger.TriggerTypeEnum;
import org.quyq.gwsu.kit.job.scheduler.type.ScheduleTypeEnum;
import org.quyq.gwsu.kit.job.service.KitJobService;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 定时任务管理服务实现
 */
@Service
@Slf4j
public class KitJobServiceImpl implements KitJobService {

    @Resource
    private KitJobGroupMapper kitJobGroupMapper;
    @Resource
    private KitJobInfoMapper kitJobInfoMapper;
    @Resource
    private KitJobLogMapper kitJobLogMapper;
    @Resource
    private KitJobLogGlueMapper kitJobLogGlueMapper;
    @Resource
    private KitJobLogReportMapper kitJobLogReportMapper;

    // ==================== 任务管理 ====================

    @Override
    public R<Map<String, Object>> pageList(int offset, int pagesize, int jobGroup, int triggerStatus, String name, String executorHandler, String author) {
        List<KitJobInfo> list = kitJobInfoMapper.pageList(offset, pagesize, jobGroup, triggerStatus, name, executorHandler, author);
        int totalCount = kitJobInfoMapper.pageListCount(offset, pagesize, jobGroup, triggerStatus, name, executorHandler, author);

        Map<String, Object> result = new HashMap<>();
        result.put("data", list);
        result.put("total", totalCount);
        return R.ok(result);
    }

    @Override
    public R<String> add(KitJobInfo jobInfo) {
        // 校验基本信息
        if (isBlank(jobInfo.getName())) {
            throw new BusinessException(KitErrorCode.E02013);
        }
        if (isBlank(jobInfo.getAuthor())) {
            throw new BusinessException(KitErrorCode.E02014);
        }

        // 校验执行器
        KitJobGroup group = kitJobGroupMapper.load(jobInfo.getJobGroup());
        if (group == null) {
            throw new BusinessException(KitErrorCode.E02015);
        }

        // 校验调度配置
        validSchedule(jobInfo);

        // 校验Glue类型
        if (GlueTypeEnum.match(jobInfo.getGlueType()) == null) {
            throw new BusinessException(KitErrorCode.E02012);
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType()) && isBlank(jobInfo.getExecutorHandler())) {
            throw new BusinessException(KitErrorCode.E02003);
        }

        // 校验高级配置
        validAdvanced(jobInfo);

        // 校验子任务ID
        validChildJobId(jobInfo, -1);

        // 写入数据库
        jobInfo.setAddTime(new Date());
        jobInfo.setUpdateTime(new Date());
        jobInfo.setGlueUpdatetime(new Date());
        jobInfo.setExecutorHandler(jobInfo.getExecutorHandler() != null ? jobInfo.getExecutorHandler().trim() : null);

        kitJobInfoMapper.save(jobInfo);
        if (jobInfo.getId() < 1) {
            return R.fail("添加任务失败");
        }

        log.info(">>>>>>>>>>> kit-job 添加任务: id = {}, name = {}", jobInfo.getId(), jobInfo.getName());
        return R.ok(String.valueOf(jobInfo.getId()));
    }

    @Override
    public R<String> update(KitJobInfo jobInfo) {
        // 校验基本信息
        if (isBlank(jobInfo.getName())) {
            throw new BusinessException(KitErrorCode.E02013);
        }
        if (isBlank(jobInfo.getAuthor())) {
            throw new BusinessException(KitErrorCode.E02014);
        }

        // 校验执行器
        if (jobInfo.getJobGroup() > 0) {
            KitJobGroup jobGroup = kitJobGroupMapper.load(jobInfo.getJobGroup());
            if (jobGroup == null) {
                throw new BusinessException(KitErrorCode.E02002);
            }
        }

        // 校验调度配置
        validSchedule(jobInfo);

        // 校验高级配置
        validAdvanced(jobInfo);

        // 校验子任务ID
        validChildJobId(jobInfo, jobInfo.getId());

        // 获取已有任务
        KitJobInfo existsJobInfo = kitJobInfoMapper.loadById(jobInfo.getId());
        if (existsJobInfo == null) {
            throw new BusinessException(KitErrorCode.E02001);
        }

        // 计算下次触发时间（5s后生效，避开预读周期）
        long nextTriggerTime = existsJobInfo.getTriggerNextTime();
        boolean scheduleDataNotChanged = jobInfo.getScheduleType() != null
                && jobInfo.getScheduleType().equals(existsJobInfo.getScheduleType())
                && jobInfo.getScheduleConf() != null
                && jobInfo.getScheduleConf().equals(existsJobInfo.getScheduleConf());

        if (existsJobInfo.getTriggerStatus() == TriggerStatus.RUNNING.getValue() && !scheduleDataNotChanged) {
            ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
            try {
                Date nextValidTime = scheduleTypeEnum.getScheduleType().generateNextTriggerTime(jobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
                if (nextValidTime == null) {
                    throw new BusinessException(KitErrorCode.E02008);
                }
                nextTriggerTime = nextValidTime.getTime();
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new BusinessException(KitErrorCode.E02008);
            }
        }

        // 更新字段
        if (jobInfo.getJobGroup() > 0) {
            existsJobInfo.setJobGroup(jobInfo.getJobGroup());
        }
        existsJobInfo.setName(jobInfo.getName());
        existsJobInfo.setAuthor(jobInfo.getAuthor());
        existsJobInfo.setAlarmEmail(jobInfo.getAlarmEmail());
        existsJobInfo.setScheduleType(jobInfo.getScheduleType());
        existsJobInfo.setScheduleConf(jobInfo.getScheduleConf());
        existsJobInfo.setMisfireStrategy(jobInfo.getMisfireStrategy());
        existsJobInfo.setExecutorRouteStrategy(jobInfo.getExecutorRouteStrategy());
        existsJobInfo.setExecutorHandler(jobInfo.getExecutorHandler() != null ? jobInfo.getExecutorHandler().trim() : null);
        existsJobInfo.setExecutorParam(jobInfo.getExecutorParam());
        existsJobInfo.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        existsJobInfo.setExecutorTimeout(jobInfo.getExecutorTimeout());
        existsJobInfo.setExecutorFailRetryCount(jobInfo.getExecutorFailRetryCount());
        existsJobInfo.setChildJobId(jobInfo.getChildJobId());
        existsJobInfo.setTriggerNextTime(nextTriggerTime);
        existsJobInfo.setUpdateTime(new Date());

        kitJobInfoMapper.update(existsJobInfo);

        log.info(">>>>>>>>>>> kit-job 更新任务: id = {}", jobInfo.getId());
        return R.ok();
    }

    @Override
    public R<String> remove(int id) {
        KitJobInfo kitJobInfo = kitJobInfoMapper.loadById(id);
        if (kitJobInfo == null) {
            return R.ok();
        }

        kitJobInfoMapper.delete(id);
        kitJobLogMapper.delete(id);
        kitJobLogGlueMapper.deleteByJobId(id);

        log.info(">>>>>>>>>>> kit-job 删除任务: id = {}", id);
        return R.ok();
    }

    @Override
    public R<String> start(int id) {
        KitJobInfo kitJobInfo = kitJobInfoMapper.loadById(id);
        if (kitJobInfo == null) {
            throw new BusinessException(KitErrorCode.E02001);
        }

        // 调度类型不能为空
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(kitJobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
        if (ScheduleTypeEnum.NONE == scheduleTypeEnum) {
            throw new BusinessException(KitErrorCode.E02021);
        }

        // 计算下次触发时间
        long nextTriggerTime;
        try {
            Date nextValidTime = scheduleTypeEnum.getScheduleType().generateNextTriggerTime(kitJobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
            if (nextValidTime == null) {
                throw new BusinessException(KitErrorCode.E02008);
            }
            nextTriggerTime = nextValidTime.getTime();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BusinessException(KitErrorCode.E02008);
        }

        kitJobInfo.setTriggerStatus(TriggerStatus.RUNNING.getValue());
        kitJobInfo.setTriggerLastTime(0);
        kitJobInfo.setTriggerNextTime(nextTriggerTime);
        kitJobInfo.setUpdateTime(new Date());
        kitJobInfoMapper.update(kitJobInfo);

        log.info(">>>>>>>>>>> kit-job 启动任务: id = {}", id);
        return R.ok();
    }

    @Override
    public R<String> stop(int id) {
        KitJobInfo kitJobInfo = kitJobInfoMapper.loadById(id);
        if (kitJobInfo == null) {
            throw new BusinessException(KitErrorCode.E02001);
        }

        kitJobInfo.setTriggerStatus(TriggerStatus.STOPPED.getValue());
        kitJobInfo.setTriggerLastTime(0);
        kitJobInfo.setTriggerNextTime(0);
        kitJobInfo.setUpdateTime(new Date());
        kitJobInfoMapper.update(kitJobInfo);

        log.info(">>>>>>>>>>> kit-job 停止任务: id = {}", id);
        return R.ok();
    }

    @Override
    public R<String> trigger(int jobId, String executorParam, String addressList) {
        KitJobInfo kitJobInfo = kitJobInfoMapper.loadById(jobId);
        if (kitJobInfo == null) {
            throw new BusinessException(KitErrorCode.E02001);
        }

        if (executorParam == null) {
            executorParam = "";
        }

        JobAdminBootstrap.getInstance().getJobTriggerPoolHelper().trigger(jobId, TriggerTypeEnum.MANUAL, -1, null, executorParam, addressList);

        log.info(">>>>>>>>>>> kit-job 手动触发任务: id = {}", jobId);
        return R.ok();
    }

    @Override
    public R<List<String>> nextTriggerTime(String scheduleType, String scheduleConf) {
        if (isBlank(scheduleType) || isBlank(scheduleConf)) {
            return R.ok(List.of());
        }

        KitJobInfo paramJobInfo = new KitJobInfo();
        paramJobInfo.setScheduleType(scheduleType);
        paramJobInfo.setScheduleConf(scheduleConf);

        List<String> result = new ArrayList<>();
        try {
            Date lastTime = new Date();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < 5; i++) {
                ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(paramJobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
                lastTime = scheduleTypeEnum.getScheduleType().generateNextTriggerTime(paramJobInfo, lastTime);
                if (lastTime != null) {
                    result.add(lastTime.toInstant().atZone(ZoneId.systemDefault()).format(formatter));
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            log.error(">>>>>>>>>>> nextTriggerTime 计算失败. scheduleType={}, scheduleConf={}, error={}", scheduleType, scheduleConf, e.getMessage());
            throw new BusinessException(KitErrorCode.E02008);
        }
        return R.ok(result);
    }

    // ==================== 执行器管理 ====================

    @Override
    public R<Map<String, Object>> groupPageList(int offset, int pagesize, String appname, String name) {
        List<KitJobGroup> list = kitJobGroupMapper.pageList(offset, pagesize, appname, name);
        int totalCount = kitJobGroupMapper.pageListCount(offset, pagesize, appname, name);

        Map<String, Object> result = new HashMap<>();
        result.put("data", list);
        result.put("total", totalCount);
        return R.ok(result);
    }

    @Override
    public R<String> groupAdd(KitJobGroup kitJobGroup) {
        // 校验AppName
        if (isBlank(kitJobGroup.getAppname())) {
            throw new BusinessException(KitErrorCode.E02022);
        }
        if (kitJobGroup.getAppname().length() < 4 || kitJobGroup.getAppname().length() > 64) {
            return R.fail("AppName长度限制4~64");
        }

        // 校验名称
        if (isBlank(kitJobGroup.getName())) {
            throw new BusinessException(KitErrorCode.E02023);
        }

        // 手动录入时校验地址
        if (kitJobGroup.getAddressType() != 0) {
            if (isBlank(kitJobGroup.getAddressList())) {
                throw new BusinessException(KitErrorCode.E02018);
            }
        }

        // 校验AppName唯一
        if (kitJobGroupMapper.loadByAppname(kitJobGroup.getAppname()) != null) {
            throw new BusinessException(KitErrorCode.E02017);
        }

        kitJobGroup.setUpdateTime(new Date());
        int ret = kitJobGroupMapper.save(kitJobGroup);
        return ret > 0 ? R.ok() : R.fail("添加执行器失败");
    }

    @Override
    public R<String> groupUpdate(KitJobGroup kitJobGroup) {
        // 校验存在
        KitJobGroup exists = kitJobGroupMapper.load(kitJobGroup.getId());
        if (exists == null) {
            throw new BusinessException(KitErrorCode.E02002);
        }

        // 校验名称
        if (isBlank(kitJobGroup.getName())) {
            throw new BusinessException(KitErrorCode.E02023);
        }

        // 手动录入时校验地址
        if (kitJobGroup.getAddressType() != 0) {
            if (isBlank(kitJobGroup.getAddressList())) {
                throw new BusinessException(KitErrorCode.E02018);
            }
        }

        kitJobGroup.setUpdateTime(new Date());
        int ret = kitJobGroupMapper.update(kitJobGroup);
        return ret > 0 ? R.ok() : R.fail("更新执行器失败");
    }

    @Override
    public R<String> groupRemove(int id) {
        KitJobGroup kitJobGroup = kitJobGroupMapper.load(id);
        if (kitJobGroup == null) {
            return R.ok();
        }

        // 执行器下是否存在任务
        int count = kitJobInfoMapper.pageListCount(0, 10, id, -1, null, null, null);
        if (count > 0) {
            throw new BusinessException(KitErrorCode.E02016);
        }

        // 至少保留一个执行器
        List<KitJobGroup> allList = kitJobGroupMapper.findAll();
        if (allList.size() == 1) {
            throw new BusinessException(KitErrorCode.E02026);
        }

        int ret = kitJobGroupMapper.remove(id);
        return ret > 0 ? R.ok() : R.fail("删除执行器失败");
    }

    @Override
    public R<KitJobGroup> groupLoadById(int id) {
        KitJobGroup jobGroup = kitJobGroupMapper.load(id);
        return jobGroup != null ? R.ok(jobGroup) : R.fail("执行器不存在");
    }

    // ==================== 日志管理 ====================

    @Override
    public R<Map<String, Object>> logPageList(int offset, int pagesize, int jobGroup, int jobId, int logStatus, Date triggerTimeStart, Date triggerTimeEnd) {
        List<KitJobLog> list = kitJobLogMapper.pageList(offset, pagesize, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
        int totalCount = kitJobLogMapper.pageListCount(offset, pagesize, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);

        Map<String, Object> result = new HashMap<>();
        result.put("data", list);
        result.put("total", totalCount);
        return R.ok(result);
    }

    @Override
    public R<KitJobLog> logLoad(long id) {
        KitJobLog kitJobLog = kitJobLogMapper.load(id);
        if (kitJobLog == null) {
            throw new BusinessException(KitErrorCode.E02024);
        }
        return R.ok(kitJobLog);
    }

    @Override
    public R<String> logClear(int jobGroup, int jobId, int type) {
        Date clearBeforeTime = null;
        int clearBeforeNum = 0;

        switch (type) {
            case 1 -> clearBeforeTime = addMonths(new Date(), -1);
            case 2 -> clearBeforeTime = addMonths(new Date(), -3);
            case 3 -> clearBeforeTime = addMonths(new Date(), -6);
            case 4 -> clearBeforeTime = addYears(new Date(), -1);
            case 5 -> clearBeforeNum = 1000;
            case 6 -> clearBeforeNum = 10000;
            case 7 -> clearBeforeNum = 30000;
            case 8 -> clearBeforeNum = 100000;
            case 9 -> clearBeforeNum = 0;
            default -> throw new BusinessException(KitErrorCode.E02025);
        }

        List<Long> logIds;
        do {
            logIds = kitJobLogMapper.findClearLogIds(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000);
            if (logIds != null && !logIds.isEmpty()) {
                kitJobLogMapper.clearLog(logIds);
            }
        } while (logIds != null && !logIds.isEmpty());

        return R.ok();
    }

    // ==================== 仪表盘 ====================

    @Override
    public R<Map<String, Object>> dashboardInfo() {
        int jobInfoCount = kitJobInfoMapper.findAllCount();
        int jobLogCount = 0;
        int jobLogSuccessCount = 0;

        KitJobLogReport logReport = kitJobLogReportMapper.queryLogReportTotal();
        if (logReport != null) {
            jobLogCount = logReport.getRunningCount() + logReport.getSucCount() + logReport.getFailCount();
            jobLogSuccessCount = logReport.getSucCount();
        }

        // 执行器数量
        Set<String> executorAddressSet = new HashSet<>();
        List<KitJobGroup> groupList = kitJobGroupMapper.findAll();
        if (groupList != null && !groupList.isEmpty()) {
            for (KitJobGroup group : groupList) {
                List<String> registryList = group.getRegistryList();
                if (registryList != null && !registryList.isEmpty()) {
                    executorAddressSet.addAll(registryList);
                }
            }
        }

        Map<String, Object> dashboardMap = new HashMap<>();
        dashboardMap.put("jobInfoCount", jobInfoCount);
        dashboardMap.put("jobLogCount", jobLogCount);
        dashboardMap.put("jobLogSuccessCount", jobLogSuccessCount);
        dashboardMap.put("executorCount", executorAddressSet.size());
        return R.ok(dashboardMap);
    }

    @Override
    public R<Map<String, Object>> chartInfo(Date startDate, Date endDate) {
        List<String> triggerDayList = new ArrayList<>();
        List<Integer> triggerDayCountRunningList = new ArrayList<>();
        List<Integer> triggerDayCountSucList = new ArrayList<>();
        List<Integer> triggerDayCountFailList = new ArrayList<>();
        int triggerCountRunningTotal = 0;
        int triggerCountSucTotal = 0;
        int triggerCountFailTotal = 0;

        List<KitJobLogReport> logReportList = kitJobLogReportMapper.queryLogReport(startDate, endDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if (logReportList != null && !logReportList.isEmpty()) {
            for (KitJobLogReport item : logReportList) {
                String day = item.getTriggerDay().toInstant().atZone(ZoneId.systemDefault()).format(formatter);
                triggerDayList.add(day);
                triggerDayCountRunningList.add(item.getRunningCount());
                triggerDayCountSucList.add(item.getSucCount());
                triggerDayCountFailList.add(item.getFailCount());
                triggerCountRunningTotal += item.getRunningCount();
                triggerCountSucTotal += item.getSucCount();
                triggerCountFailTotal += item.getFailCount();
            }
        } else {
            for (int i = -6; i <= 0; i++) {
                triggerDayList.add(LocalDate.now().plusDays(i).format(formatter));
                triggerDayCountRunningList.add(0);
                triggerDayCountSucList.add(0);
                triggerDayCountFailList.add(0);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("triggerDayList", triggerDayList);
        result.put("triggerDayCountRunningList", triggerDayCountRunningList);
        result.put("triggerDayCountSucList", triggerDayCountSucList);
        result.put("triggerDayCountFailList", triggerDayCountFailList);
        result.put("triggerCountRunningTotal", triggerCountRunningTotal);
        result.put("triggerCountSucTotal", triggerCountSucTotal);
        result.put("triggerCountFailTotal", triggerCountFailTotal);
        return R.ok(result);
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 校验调度配置
     */
    private void validSchedule(KitJobInfo jobInfo) {
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (scheduleTypeEnum == null) {
            throw new BusinessException(KitErrorCode.E02008);
        }
        if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
            if (jobInfo.getScheduleConf() == null || !CronExpression.isValidExpression(jobInfo.getScheduleConf())) {
                throw new BusinessException(KitErrorCode.E02004);
            }
        } else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE) {
            if (jobInfo.getScheduleConf() == null) {
                throw new BusinessException(KitErrorCode.E02008);
            }
            try {
                int fixSecond = Integer.parseInt(jobInfo.getScheduleConf());
                if (fixSecond < 1) {
                    throw new BusinessException(KitErrorCode.E02008);
                }
            } catch (NumberFormatException e) {
                throw new BusinessException(KitErrorCode.E02008);
            }
        }
    }

    /**
     * 校验高级配置
     */
    private void validAdvanced(KitJobInfo jobInfo) {
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            throw new BusinessException(KitErrorCode.E02011);
        }
        if (MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), null) == null) {
            throw new BusinessException(KitErrorCode.E02009);
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            throw new BusinessException(KitErrorCode.E02010);
        }
    }

    /**
     * 校验子任务ID
     *
     * @param jobInfo     任务信息
     * @param excludeId   排除的任务ID（更新时排除自身）
     */
    private void validChildJobId(KitJobInfo jobInfo, int excludeId) {
        if (isNotBlank(jobInfo.getChildJobId())) {
            String[] childJobIds = jobInfo.getChildJobId().split(",");
            for (String childJobIdItem : childJobIds) {
                if (isNotBlank(childJobIdItem) && isNumeric(childJobIdItem)) {
                    int childJobId = Integer.parseInt(childJobIdItem);
                    if (childJobId == excludeId) {
                        throw new BusinessException(KitErrorCode.E02020);
                    }
                    KitJobInfo childJobInfo = kitJobInfoMapper.loadById(childJobId);
                    if (childJobInfo == null) {
                        throw new BusinessException(KitErrorCode.E02019);
                    }
                } else {
                    throw new BusinessException(KitErrorCode.E02020);
                }
            }

            // 去掉多余逗号
            StringJoiner joiner = new StringJoiner(",");
            for (String item : childJobIds) {
                if (isNotBlank(item)) {
                    joiner.add(item);
                }
            }
            jobInfo.setChildJobId(joiner.toString());
        }
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private static Date addMonths(Date date, int months) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, months);
        return calendar.getTime();
    }

    private static Date addYears(Date date, int years) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.YEAR, years);
        return calendar.getTime();
    }

}
