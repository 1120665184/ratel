package org.quyq.gwsu.kit.job.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.service.KitJobService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 定时任务管理控制器
 */
@RestController
@RequestMapping("/job/info")
@Tag(name = "定时任务管理")
@RequiredArgsConstructor
public class JobInfoController {

    private final KitJobService kitJobService;

    @PostMapping("pageList")
    @Operation(summary = "分页查询任务列表")
    public R<Map<String, Object>> pageList(@RequestParam(defaultValue = "0") int offset,
                                            @RequestParam(defaultValue = "10") int pagesize,
                                            @RequestParam(defaultValue = "0") int jobGroup,
                                            @RequestParam(defaultValue = "-1") int triggerStatus,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) String executorHandler,
                                            @RequestParam(required = false) String author) {
        return kitJobService.pageList(offset, pagesize, jobGroup, triggerStatus, name, executorHandler, author);
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
    public R<String> remove(@RequestParam("id") int id) {
        return kitJobService.remove(id);
    }

    @PostMapping("start")
    @Operation(summary = "启动任务")
    public R<String> start(@RequestParam("id") int id) {
        return kitJobService.start(id);
    }

    @PostMapping("stop")
    @Operation(summary = "停止任务")
    public R<String> stop(@RequestParam("id") int id) {
        return kitJobService.stop(id);
    }

    @PostMapping("trigger")
    @Operation(summary = "手动触发一次任务")
    public R<String> trigger(@RequestParam("id") int id,
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

}
