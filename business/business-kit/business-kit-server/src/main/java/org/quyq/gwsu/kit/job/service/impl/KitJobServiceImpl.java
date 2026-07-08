package org.quyq.gwsu.kit.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.job.constant.ExecutorBlockStrategyEnum;
import org.quyq.gwsu.common.job.glue.GlueTypeEnum;
import org.quyq.gwsu.common.job.openapi.executor.dto.KillRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogData;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogRequest;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.kit.api.job.dto.JobInfoCreateDTO;
import org.quyq.gwsu.kit.api.job.dto.KitJobInfoDTO;
import org.quyq.gwsu.kit.api.job.dto.KitJobLogDTO;
import org.quyq.gwsu.kit.api.job.vo.UrlHandlerParam;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.domain.KitJobLog;
import org.quyq.gwsu.kit.job.domain.KitJobLogGlue;
import org.quyq.gwsu.kit.job.domain.KitJobLogReport;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.quyq.gwsu.kit.job.scheduler.constant.TriggerStatus;
import org.quyq.gwsu.kit.job.scheduler.cron.CronExpression;
import org.quyq.gwsu.kit.job.scheduler.misfire.MisfireStrategyEnum;
import org.quyq.gwsu.kit.job.scheduler.route.ExecutorRouteStrategyEnum;
import org.quyq.gwsu.kit.job.scheduler.thread.HandlerRegistryInfo;
import org.quyq.gwsu.kit.job.scheduler.thread.JobScheduleHelper;
import org.quyq.gwsu.kit.job.scheduler.trigger.TriggerTypeEnum;
import org.quyq.gwsu.kit.job.scheduler.type.ScheduleTypeEnum;
import org.quyq.gwsu.kit.job.service.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * 定时任务管理服务实现
 */
@Service
@Slf4j
public class KitJobServiceImpl implements KitJobService {

    private static final String GLUE_REMARK_INIT = "GLUE代码初始化";
    private static final String GLUE_REMARK_UPDATE = "GLUE代码更新";

    @Resource
    private IKitJobInfoService kitJobInfoService;
    @Resource
    private IKitJobLogService kitJobLogService;
    @Resource
    private IKitJobLogGlueService kitJobLogGlueService;
    @Resource
    private IKitJobLogReportService kitJobLogReportService;
    @Resource
    private SecurityUtils securityUtils;

    // ==================== 任务管理 ====================

    @Override
    public R<IPage<KitJobInfo>> pageList(KitJobInfoDTO dto) {
        LambdaQueryWrapper<KitJobInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dto.getTriggerStatus() != null && dto.getTriggerStatus() >= 0, KitJobInfo::getTriggerStatus, dto.getTriggerStatus())
                .like(StringUtils.hasText(dto.getName()), KitJobInfo::getName, dto.getName())
                .like(StringUtils.hasText(dto.getExecutorHandler()), KitJobInfo::getExecutorHandler, dto.getExecutorHandler())
                .like(StringUtils.hasText(dto.getAuthor()), KitJobInfo::getAuthor, dto.getAuthor())
                .orderByDesc(KitJobInfo::getId);

        IPage<KitJobInfo> page = kitJobInfoService.page(Page.of(dto.getPageNum(), dto.getPageSize()), wrapper);
        return R.ok(page);
    }

    @Override
    public R<String> add(KitJobInfo jobInfo) {
        if (!StringUtils.hasText(jobInfo.getName())) {
            throw new BusinessException(KitErrorCode.E02013);
        }
        if (!StringUtils.hasText(jobInfo.getAuthor())) {
            throw new BusinessException(KitErrorCode.E02014);
        }

        validSchedule(jobInfo);

        if (GlueTypeEnum.match(jobInfo.getGlueType()) == null) {
            throw new BusinessException(KitErrorCode.E02012);
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType()) && !StringUtils.hasText(jobInfo.getExecutorHandler())) {
            throw new BusinessException(KitErrorCode.E02003);
        }

        validAdvanced(jobInfo);

        validChildJobId(jobInfo, null);

        jobInfo.setGlueUpdatetime(LocalDateTime.now());
        jobInfo.setExecutorHandler(jobInfo.getExecutorHandler() != null ? jobInfo.getExecutorHandler().trim() : null);

        kitJobInfoService.save(jobInfo);
        if (!StringUtils.hasText(jobInfo.getId())) {
            return R.fail("添加任务失败");
        }

        log.info(">>>>>>>>>>> kit-job 添加任务: id = {}, name = {}", jobInfo.getId(), jobInfo.getName());
        return R.ok(jobInfo.getId());
    }

    @Override
    public R<String> update(KitJobInfo jobInfo) {
        if (!StringUtils.hasText(jobInfo.getName())) {
            throw new BusinessException(KitErrorCode.E02013);
        }
        if (!StringUtils.hasText(jobInfo.getAuthor())) {
            throw new BusinessException(KitErrorCode.E02014);
        }

        validSchedule(jobInfo);
        validAdvanced(jobInfo);
        validChildJobId(jobInfo, jobInfo.getId());

        KitJobInfo existsJobInfo = kitJobInfoService.getById(jobInfo.getId());
        if (existsJobInfo == null) {
            throw new BusinessException(KitErrorCode.E02001);
        }

        long nextTriggerTime = existsJobInfo.getTriggerNextTime();
        boolean scheduleDataNotChanged = jobInfo.getScheduleType() != null
                && jobInfo.getScheduleType().equals(existsJobInfo.getScheduleType())
                && jobInfo.getScheduleConf() != null
                && jobInfo.getScheduleConf().equals(existsJobInfo.getScheduleConf());

        if (existsJobInfo.getTriggerStatus() == TriggerStatus.RUNNING.getValue() && !scheduleDataNotChanged) {
            ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
            try {
                LocalDateTime nextValidTime = scheduleTypeEnum.getScheduleType().generateNextTriggerTime(jobInfo, LocalDateTime.now().plusSeconds(JobScheduleHelper.PRE_READ_MS / 1000));
                if (nextValidTime == null) {
                    throw new BusinessException(KitErrorCode.E02008);
                }
                nextTriggerTime = nextValidTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new BusinessException(KitErrorCode.E02008);
            }
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
        boolean glueChanged = !Objects.equals(existsJobInfo.getGlueType(), jobInfo.getGlueType())
                || !Objects.equals(existsJobInfo.getGlueSource(), jobInfo.getGlueSource())
                || !Objects.equals(existsJobInfo.getGlueRemark(), jobInfo.getGlueRemark());
        existsJobInfo.setGlueType(jobInfo.getGlueType());
        existsJobInfo.setGlueSource(jobInfo.getGlueSource());
        existsJobInfo.setGlueRemark(jobInfo.getGlueRemark());
        if (glueChanged) {
            existsJobInfo.setGlueUpdatetime(LocalDateTime.now());
        }

        kitJobInfoService.updateById(existsJobInfo);

        log.info(">>>>>>>>>>> kit-job 更新任务: id = {}", jobInfo.getId());
        return R.ok();
    }

    @Override
    public R<String> remove(String id) {
        KitJobInfo kitJobInfo = kitJobInfoService.getById(id);
        if (kitJobInfo == null) {
            return R.ok();
        }

        kitJobInfoService.removeById(id);
        kitJobLogService.remove(new LambdaQueryWrapper<KitJobLog>().eq(KitJobLog::getJobId, id));
        kitJobLogGlueService.remove(new LambdaQueryWrapper<KitJobLogGlue>().eq(KitJobLogGlue::getJobId, id));

        log.info(">>>>>>>>>>> kit-job 删除任务: id = {}", id);
        return R.ok();
    }

    @Override
    public R<String> start(String id) {
        KitJobInfo kitJobInfo = kitJobInfoService.getById(id);
        if (kitJobInfo == null) {
            throw new BusinessException(KitErrorCode.E02001);
        }

        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(kitJobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
        if (ScheduleTypeEnum.NONE == scheduleTypeEnum) {
            throw new BusinessException(KitErrorCode.E02021);
        }

        long nextTriggerTime;
        try {
            LocalDateTime nextValidTime = scheduleTypeEnum.getScheduleType().generateNextTriggerTime(kitJobInfo, LocalDateTime.now().plusSeconds(JobScheduleHelper.PRE_READ_MS / 1000));
            if (nextValidTime == null) {
                throw new BusinessException(KitErrorCode.E02008);
            }
            nextTriggerTime = nextValidTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BusinessException(KitErrorCode.E02008);
        }

        kitJobInfo.setTriggerStatus(TriggerStatus.RUNNING.getValue());
        kitJobInfo.setTriggerLastTime(0);
        kitJobInfo.setTriggerNextTime(nextTriggerTime);
        kitJobInfoService.updateById(kitJobInfo);

        log.info(">>>>>>>>>>> kit-job 启动任务: id = {}", id);
        return R.ok();
    }

    @Override
    public R<String> stop(String id) {
        KitJobInfo kitJobInfo = kitJobInfoService.getById(id);
        if (kitJobInfo == null) {
            throw new BusinessException(KitErrorCode.E02001);
        }

        kitJobInfo.setTriggerStatus(TriggerStatus.STOPPED.getValue());
        kitJobInfo.setTriggerLastTime(0);
        kitJobInfo.setTriggerNextTime(0);
        kitJobInfoService.updateById(kitJobInfo);

        log.info(">>>>>>>>>>> kit-job 停止任务: id = {}", id);
        return R.ok();
    }

    @Override
    public R<String> trigger(String jobId, String executorParam, String addressList) {
        KitJobInfo kitJobInfo = kitJobInfoService.getById(jobId);
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
        if (!StringUtils.hasText(scheduleType) || !StringUtils.hasText(scheduleConf)) {
            return R.ok(List.of());
        }

        KitJobInfo paramJobInfo = new KitJobInfo();
        paramJobInfo.setScheduleType(scheduleType);
        paramJobInfo.setScheduleConf(scheduleConf);

        List<String> result = new ArrayList<>();
        try {
            LocalDateTime lastTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < 5; i++) {
                ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(paramJobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
                lastTime = scheduleTypeEnum.getScheduleType().generateNextTriggerTime(paramJobInfo, lastTime);
                if (lastTime != null) {
                    result.add(lastTime.format(formatter));
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

    // ==================== 调度界面适配 ====================

    @Override
    public R<String> addByDTO(JobInfoCreateDTO dto) {
        KitJobInfo jobInfo = new KitJobInfo();

        // 公共字段
        jobInfo.setName(dto.getName());
        jobInfo.setAuthor(currentUserName(dto.getAuthor()));
        jobInfo.setAlarmEmail(dto.getAlarmEmail());
        jobInfo.setMisfireStrategy(dto.getMisfireStrategy());
        jobInfo.setExecutorRouteStrategy(dto.getExecutorRouteStrategy());
        jobInfo.setExecutorBlockStrategy(dto.getExecutorBlockStrategy());
        jobInfo.setExecutorTimeout(dto.getExecutorTimeout());
        jobInfo.setExecutorFailRetryCount(dto.getExecutorFailRetryCount());
        jobInfo.setScheduleType(dto.getScheduleType());
        jobInfo.setScheduleConf(dto.getScheduleConf());
        jobInfo.setChildJobId(dto.getChildJobId());

        // 按模式映射
        String jobMode = dto.getJobMode();
        if ("URL".equals(jobMode)) {
            jobInfo.setGlueType(GlueTypeEnum.BEAN.name());
            jobInfo.setExecutorHandler("urlJobHandler");
            UrlHandlerParam param = new UrlHandlerParam(dto.getPrefix(), dto.getUrl(), dto.getBodyJson());
            jobInfo.setExecutorParam(new Gson().toJson(param));
        } else if ("BEAN".equals(jobMode)) {
            jobInfo.setGlueType(GlueTypeEnum.BEAN.name());
            jobInfo.setExecutorHandler(dto.getExecutorHandler());
            jobInfo.setExecutorParam(dto.getExecutorParam());
        } else if ("GLUE".equals(jobMode)) {
            jobInfo.setGlueType(dto.getGlueType());
            jobInfo.setGlueSource(dto.getGlueSource());
            jobInfo.setGlueRemark(normalizeGlueRemark(dto.getGlueRemark(), GLUE_REMARK_INIT));
        } else {
            throw new BusinessException(KitErrorCode.E02012);
        }

        return add(jobInfo);
    }

    @Override
    public R<String> updateByDTO(JobInfoCreateDTO dto) {
        KitJobInfo jobInfo = new KitJobInfo();
        jobInfo.setId(dto.getId());

        // 公共字段
        jobInfo.setName(dto.getName());
        jobInfo.setAuthor(currentUserName(dto.getAuthor()));
        jobInfo.setAlarmEmail(dto.getAlarmEmail());
        jobInfo.setMisfireStrategy(dto.getMisfireStrategy());
        jobInfo.setExecutorRouteStrategy(dto.getExecutorRouteStrategy());
        jobInfo.setExecutorBlockStrategy(dto.getExecutorBlockStrategy());
        jobInfo.setExecutorTimeout(dto.getExecutorTimeout());
        jobInfo.setExecutorFailRetryCount(dto.getExecutorFailRetryCount());
        jobInfo.setScheduleType(dto.getScheduleType());
        jobInfo.setScheduleConf(dto.getScheduleConf());
        jobInfo.setChildJobId(dto.getChildJobId());

        // 按模式映射
        String jobMode = dto.getJobMode();
        if ("URL".equals(jobMode)) {
            jobInfo.setGlueType(GlueTypeEnum.BEAN.name());
            jobInfo.setExecutorHandler("urlJobHandler");
            UrlHandlerParam param = new UrlHandlerParam(dto.getPrefix(), dto.getUrl(), dto.getBodyJson());
            jobInfo.setExecutorParam(new Gson().toJson(param));
        } else if ("BEAN".equals(jobMode)) {
            jobInfo.setGlueType(GlueTypeEnum.BEAN.name());
            jobInfo.setExecutorHandler(dto.getExecutorHandler());
            jobInfo.setExecutorParam(dto.getExecutorParam());
        } else if ("GLUE".equals(jobMode)) {
            jobInfo.setGlueType(dto.getGlueType());
            jobInfo.setGlueSource(dto.getGlueSource());
            jobInfo.setGlueRemark(normalizeGlueRemark(dto.getGlueRemark(), GLUE_REMARK_UPDATE));

            // GLUE更新时保存历史版本
            KitJobInfo existsJobInfo = kitJobInfoService.getById(dto.getId());
            if (existsJobInfo != null && StringUtils.hasText(existsJobInfo.getGlueSource())) {
                KitJobLogGlue glueLog = new KitJobLogGlue();
                glueLog.setJobId(dto.getId());
                glueLog.setGlueType(existsJobInfo.getGlueType());
                glueLog.setGlueSource(existsJobInfo.getGlueSource());
                glueLog.setGlueRemark(normalizeGlueRemark(existsJobInfo.getGlueRemark(), GLUE_REMARK_UPDATE));
                kitJobLogGlueService.save(glueLog);
                kitJobLogGlueService.removeOld(dto.getId(), 30);
            }
        } else {
            throw new BusinessException(KitErrorCode.E02012);
        }

        return update(jobInfo);
    }

    @Override
    public R<String> kill(String logId) {
        KitJobLog kitJobLog = kitJobLogService.getById(logId);
        if (kitJobLog == null) {
            throw new BusinessException(KitErrorCode.E02024);
        }

        String address = kitJobLog.getExecutorAddress();
        if (!StringUtils.hasText(address)) {
            return R.fail("执行器地址为空，无法终止");
        }

        KillRequest killRequest = new KillRequest(kitJobLog.getJobId());
        R<String> killResult = JobAdminBootstrap.getInstance().getTriggerStrategy().kill(address, killRequest);

        if (killResult != null && killResult.isSuccess()) {
            log.info(">>>>>>>>>>> kit-job 终止任务成功: logId={}", logId);
            return R.ok();
        } else {
            return R.fail("终止任务失败：" + (killResult != null ? killResult.msg() : "未知错误"));
        }
    }

    @Override
    public R<LogData> logContent(String logId, int fromLineNum) {
        KitJobLog kitJobLog = kitJobLogService.getById(logId);
        if (kitJobLog == null) {
            throw new BusinessException(KitErrorCode.E02024);
        }

        String address = kitJobLog.getExecutorAddress();
        if (!StringUtils.hasText(address)) {
            return R.fail("执行器地址为空");
        }

        long logDateTime = kitJobLog.getTriggerTime() != null
                ? kitJobLog.getTriggerTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis();

        LogRequest logRequest = new LogRequest(logId, logDateTime, fromLineNum);
        R<LogData> logResult = JobAdminBootstrap.getInstance().getTriggerStrategy().log(address, logRequest);
        if (logResult != null && logResult.isSuccess()) {
            LogData data = logResult.data();
            if (data != null
                    && kitJobLog.getHandleCode() != 0
                    && !data.isEnd()
                    && (!StringUtils.hasText(data.getLogContent()) || data.getToLineNum() < fromLineNum)) {
                data.setEnd(true);
            }
            return R.ok(logResult.data());
        } else {
            return R.fail("读取执行日志失败：" + (logResult != null ? logResult.msg() : "未知错误"));
        }
    }

    @Override
    public R<List<String>> handlerList() {
        Map<String, HandlerRegistryInfo> allHandlerRegistry = JobAdminBootstrap.getInstance()
                .getJobRegistryHelper().getAllHandlerRegistry();
        List<String> result = new ArrayList<>();
        if (allHandlerRegistry != null) {
            for (HandlerRegistryInfo info : allHandlerRegistry.values()) {
                if (!"urlJobHandler".equals(info.handlerName())) {
                    result.add(info.handlerName());
                }
            }
        }
        return R.ok(result);
    }

    @Override
    public R<List<KitJobLogGlue>> glueVersionList(String jobId) {
        List<KitJobLogGlue> list = kitJobLogGlueService.list(
                new LambdaQueryWrapper<KitJobLogGlue>()
                        .eq(KitJobLogGlue::getJobId, jobId)
                        .orderByDesc(KitJobLogGlue::getId));
        return R.ok(list);
    }

    @Override
    public R<KitJobLogGlue> glueVersionDetail(String id) {
        KitJobLogGlue glueLog = kitJobLogGlueService.getById(id);
        if (glueLog == null) {
            return R.fail("GLUE版本记录不存在");
        }
        return R.ok(glueLog);
    }

    // ==================== 日志管理 ====================

    @Override
    public R<IPage<KitJobLog>> logPageList(KitJobLogDTO dto) {
        LambdaQueryWrapper<KitJobLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(dto.getJobId()), KitJobLog::getJobId, dto.getJobId())
                .ge(dto.getTriggerTimeStart() != null, KitJobLog::getTriggerTime, dto.getTriggerTimeStart())
                .le(dto.getTriggerTimeEnd() != null, KitJobLog::getTriggerTime, dto.getTriggerTimeEnd());

        if (dto.getLogStatus() != null) {
            if (dto.getLogStatus() == 1) {
                wrapper.eq(KitJobLog::getHandleCode, 200);
            } else if (dto.getLogStatus() == 2) {
                wrapper.and(w -> w
                        .notIn(KitJobLog::getTriggerCode, 0, 200)
                        .or()
                        .notIn(KitJobLog::getHandleCode, 0, 200));
            } else if (dto.getLogStatus() == 3) {
                wrapper.eq(KitJobLog::getTriggerCode, 200)
                        .eq(KitJobLog::getHandleCode, 0);
            }
        }

        wrapper.orderByDesc(KitJobLog::getId);

        IPage<KitJobLog> page = kitJobLogService.page(Page.of(dto.getPageNum(), dto.getPageSize()), wrapper);
        return R.ok(page);
    }

    @Override
    public R<KitJobLog> logLoad(String id) {
        KitJobLog kitJobLog = kitJobLogService.getById(id);
        if (kitJobLog == null) {
            throw new BusinessException(KitErrorCode.E02024);
        }
        return R.ok(kitJobLog);
    }

    @Override
    public R<String> logClear(String jobId, int type) {
        LocalDateTime clearBeforeTime = null;
        int clearBeforeNum = 0;

        switch (type) {
            case 1 -> clearBeforeTime = LocalDateTime.now().minusMonths(1);
            case 2 -> clearBeforeTime = LocalDateTime.now().minusMonths(3);
            case 3 -> clearBeforeTime = LocalDateTime.now().minusMonths(6);
            case 4 -> clearBeforeTime = LocalDateTime.now().minusYears(1);
            case 5 -> clearBeforeNum = 1000;
            case 6 -> clearBeforeNum = 10000;
            case 7 -> clearBeforeNum = 30000;
            case 8 -> clearBeforeNum = 100000;
            case 9 -> clearBeforeNum = 0;
            default -> throw new BusinessException(KitErrorCode.E02025);
        }

        List<String> logIds;
        do {
            logIds = kitJobLogService.findClearLogIds(jobId, clearBeforeTime, clearBeforeNum, 1000);
            if (logIds != null && !logIds.isEmpty()) {
                kitJobLogService.clearLog(logIds);
            }
        } while (logIds != null && !logIds.isEmpty());

        return R.ok();
    }

    // ==================== 仪表盘 ====================

    @Override
    public R<Map<String, Object>> dashboardInfo() {
        Long jobInfoCount = kitJobInfoService.count();
        int jobLogCount = 0;
        int jobLogSuccessCount = 0;

        KitJobLogReport logReport = kitJobLogReportService.queryLogReportTotal();
        if (logReport != null) {
            jobLogCount = logReport.getRunningCount() + logReport.getSucCount() + logReport.getFailCount();
            jobLogSuccessCount = logReport.getSucCount();
        }

        // 从 handler 注册缓存获取执行器地址
        Set<String> executorAddressSet = new HashSet<>();
        Map<String, org.quyq.gwsu.kit.job.scheduler.thread.HandlerRegistryInfo> handlerRegistry = JobAdminBootstrap.getInstance().getJobRegistryHelper().getAllHandlerRegistry();
        if (handlerRegistry != null && !handlerRegistry.isEmpty()) {
            for (org.quyq.gwsu.kit.job.scheduler.thread.HandlerRegistryInfo info : handlerRegistry.values()) {
                if (info.addresses() != null) {
                    executorAddressSet.addAll(info.addresses());
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
    public R<Map<String, Object>> chartInfo(LocalDateTime startDate, LocalDateTime endDate) {
        List<String> triggerDayList = new ArrayList<>();
        List<Integer> triggerDayCountRunningList = new ArrayList<>();
        List<Integer> triggerDayCountSucList = new ArrayList<>();
        List<Integer> triggerDayCountFailList = new ArrayList<>();
        int triggerCountRunningTotal = 0;
        int triggerCountSucTotal = 0;
        int triggerCountFailTotal = 0;

        List<KitJobLogReport> logReportList = kitJobLogReportService.list(
                new LambdaQueryWrapper<KitJobLogReport>()
                        .between(KitJobLogReport::getTriggerDay, startDate, endDate)
                        .orderByAsc(KitJobLogReport::getTriggerDay));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if (logReportList != null && !logReportList.isEmpty()) {
            for (KitJobLogReport item : logReportList) {
                String day = item.getTriggerDay().format(formatter);
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

    private String currentUserName(String author) {
        if (StringUtils.hasText(author)) {
            return author;
        }
        return securityUtils.userInfo()
                .map(UserInfo::getUserName)
                .orElse("");
    }

    private String normalizeGlueRemark(String glueRemark, String defaultRemark) {
        return StringUtils.hasText(glueRemark) ? glueRemark.trim() : defaultRemark;
    }

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
     * 校验子任务ID（主键已改为String）
     */
    private void validChildJobId(KitJobInfo jobInfo, String excludeId) {
        if (StringUtils.hasText(jobInfo.getChildJobId())) {
            String[] childJobIds = jobInfo.getChildJobId().split(",");
            for (String childJobIdItem : childJobIds) {
                if (StringUtils.hasText(childJobIdItem)) {
                    // 子任务ID现在是雪花ID字符串
                    if (childJobIdItem.equals(excludeId)) {
                        throw new BusinessException(KitErrorCode.E02020);
                    }
                    KitJobInfo childJobInfo = kitJobInfoService.getById(childJobIdItem.trim());
                    if (childJobInfo == null) {
                        throw new BusinessException(KitErrorCode.E02019);
                    }
                } else {
                    throw new BusinessException(KitErrorCode.E02020);
                }
            }

            StringJoiner joiner = new StringJoiner(",");
            for (String item : childJobIds) {
                if (StringUtils.hasText(item)) {
                    joiner.add(item);
                }
            }
            jobInfo.setChildJobId(joiner.toString());
        }
    }

}
