package org.quyq.gwsu.kit.job.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogData;
import org.quyq.gwsu.kit.api.job.dto.JobInfoCreateDTO;
import org.quyq.gwsu.kit.api.job.dto.KitJobInfoDTO;
import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.domain.KitJobLogGlue;
import org.quyq.gwsu.kit.job.service.KitJobService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 定时任务管理控制器
 */
@RestController
@RequestMapping("/job/info")
@Tag(name = "定时任务管理")
@RequiredArgsConstructor
public class JobInfoController {

    private final KitJobService kitJobService;

    @PostMapping("page")
    @Operation(summary = "分页查询任务列表")
    public R<IPage<KitJobInfo>> pageByCondition(@RequestBody KitJobInfoDTO dto) {
        return kitJobService.pageList(dto);
    }

    @PostMapping("add")
    @Operation(summary = "添加任务")
    public R<String> add(@RequestBody KitJobInfo jobInfo) {
        return kitJobService.add(jobInfo);
    }

    @PostMapping("update")
    @Operation(summary = "更新任务")
    public R<String> update(@RequestBody KitJobInfo jobInfo) {
        return kitJobService.update(jobInfo);
    }

    @PostMapping("remove")
    @Operation(summary = "删除任务")
    public R<String> remove(@RequestParam("id") String id) {
        return kitJobService.remove(id);
    }

    @PostMapping("start")
    @Operation(summary = "启动任务")
    public R<String> start(@RequestParam("id") String id) {
        return kitJobService.start(id);
    }

    @PostMapping("stop")
    @Operation(summary = "停止任务")
    public R<String> stop(@RequestParam("id") String id) {
        return kitJobService.stop(id);
    }

    @PostMapping("trigger")
    @Operation(summary = "手动触发一次任务")
    public R<String> trigger(@RequestParam("id") String id,
                              @RequestParam(value = "executorParam", required = false, defaultValue = "") String executorParam,
                              @RequestParam(value = "addressList", required = false, defaultValue = "") String addressList) {
        return kitJobService.trigger(id, executorParam, addressList);
    }

    @GetMapping("nextTriggerTime")
    @Operation(summary = "预估下次触发时间")
    public R<List<String>> nextTriggerTime(@RequestParam("scheduleType") String scheduleType,
                                            @RequestParam("scheduleConf") String scheduleConf) {
        return kitJobService.nextTriggerTime(scheduleType, scheduleConf);
    }

    @PostMapping("addByDTO")
    @Operation(summary = "DTO适配添加任务")
    public R<String> addByDTO(@RequestBody JobInfoCreateDTO dto) {
        return kitJobService.addByDTO(dto);
    }

    @PostMapping("updateByDTO")
    @Operation(summary = "DTO适配更新任务")
    public R<String> updateByDTO(@RequestBody JobInfoCreateDTO dto) {
        return kitJobService.updateByDTO(dto);
    }

    @PostMapping("kill")
    @Operation(summary = "终止运行中的任务")
    public R<String> kill(@RequestParam("logId") String logId) {
        return kitJobService.kill(logId);
    }

    @GetMapping("logContent")
    @Operation(summary = "读取执行器端完整日志")
    public R<LogData> logContent(@RequestParam("logId") String logId,
                              @RequestParam(value = "fromLineNum", defaultValue = "1") int fromLineNum) {
        return kitJobService.logContent(logId, fromLineNum);
    }

    @GetMapping("handlerList")
    @Operation(summary = "查询所有在线Handler名称(过滤urlJobHandler)")
    public R<List<String>> handlerList() {
        return kitJobService.handlerList();
    }

    @GetMapping("glueVersionList")
    @Operation(summary = "查询GLUE版本历史")
    public R<List<KitJobLogGlue>> glueVersionList(@RequestParam("jobId") String jobId) {
        return kitJobService.glueVersionList(jobId);
    }

    @GetMapping("glueVersionDetail")
    @Operation(summary = "查询GLUE版本详情")
    public R<KitJobLogGlue> glueVersionDetail(@RequestParam("id") String id) {
        return kitJobService.glueVersionDetail(id);
    }

}
