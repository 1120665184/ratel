package org.quyq.gwsu.kit.job.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.job.domain.KitJobLog;
import org.quyq.gwsu.kit.job.service.KitJobService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

/**
 * 任务日志控制器
 */
@RestController
@RequestMapping("/job/log")
@Tag(name = "任务日志管理")
@RequiredArgsConstructor
public class JobLogController {

    private final KitJobService kitJobService;

    @PostMapping("pageList")
    @Operation(summary = "分页查询日志列表")
    public R<Map<String, Object>> pageList(@RequestParam(defaultValue = "0") int offset,
                                            @RequestParam(defaultValue = "10") int pagesize,
                                            @RequestParam(defaultValue = "0") int jobGroup,
                                            @RequestParam(defaultValue = "0") int jobId,
                                            @RequestParam(defaultValue = "0") int logStatus,
                                            @RequestParam(value = "triggerTimeStart", required = false)
                                            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date triggerTimeStart,
                                            @RequestParam(value = "triggerTimeEnd", required = false)
                                            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date triggerTimeEnd) {
        return kitJobService.logPageList(offset, pagesize, jobGroup, jobId, logStatus, triggerTimeStart, triggerTimeEnd);
    }

    @GetMapping("load")
    @Operation(summary = "查询日志详情")
    public R<KitJobLog> load(@RequestParam("id") long id) {
        return kitJobService.logLoad(id);
    }

    @PostMapping("clearLog")
    @Operation(summary = "清理日志")
    public R<String> clearLog(@RequestParam("jobGroup") int jobGroup,
                               @RequestParam("jobId") int jobId,
                               @RequestParam("type") int type) {
        return kitJobService.logClear(jobGroup, jobId, type);
    }

    @GetMapping("dashboardInfo")
    @Operation(summary = "仪表盘概览信息")
    public R<Map<String, Object>> dashboardInfo() {
        return kitJobService.dashboardInfo();
    }

    @GetMapping("chartInfo")
    @Operation(summary = "仪表盘图表数据")
    public R<Map<String, Object>> chartInfo(@RequestParam(value = "startDate", required = false)
                                             @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startDate,
                                             @RequestParam(value = "endDate", required = false)
                                             @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endDate) {
        return kitJobService.chartInfo(startDate, endDate);
    }

}
