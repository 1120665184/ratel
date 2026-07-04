package org.quyq.gwsu.kit.job.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.api.job.dto.KitJobLogDTO;
import org.quyq.gwsu.kit.job.domain.KitJobLog;
import org.quyq.gwsu.kit.job.service.KitJobService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @PostMapping("page")
    @Operation(summary = "分页查询日志列表")
    public R<IPage<KitJobLog>> pageByCondition(@RequestBody KitJobLogDTO dto) {
        return kitJobService.logPageList(dto);
    }

    @GetMapping("load")
    @Operation(summary = "查询日志详情")
    public R<KitJobLog> load(@RequestParam("id") String id) {
        return kitJobService.logLoad(id);
    }

    @PostMapping("clearLog")
    @Operation(summary = "清理日志")
    public R<String> clearLog(@RequestParam("jobGroup") String jobGroup,
                               @RequestParam("jobId") String jobId,
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
                                             @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
                                             @RequestParam(value = "endDate", required = false)
                                             @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        return kitJobService.chartInfo(startDate, endDate);
    }

}
