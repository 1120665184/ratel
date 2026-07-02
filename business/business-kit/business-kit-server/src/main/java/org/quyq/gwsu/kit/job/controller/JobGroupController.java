package org.quyq.gwsu.kit.job.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.job.domain.KitJobGroup;
import org.quyq.gwsu.kit.job.service.KitJobService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 执行器管理控制器
 */
@RestController
@RequestMapping("/job/group")
@Tag(name = "执行器管理")
@RequiredArgsConstructor
public class JobGroupController {

    private final KitJobService kitJobService;

    @PostMapping("pageList")
    @Operation(summary = "分页查询执行器列表")
    public R<Map<String, Object>> pageList(@RequestParam(defaultValue = "0") int offset,
                                            @RequestParam(defaultValue = "10") int pagesize,
                                            @RequestParam(required = false) String appname,
                                            @RequestParam(required = false) String name) {
        return kitJobService.groupPageList(offset, pagesize, appname, name);
    }

    @PostMapping("add")
    @Operation(summary = "添加执行器")
    public R<String> add(@RequestBody KitJobGroup kitJobGroup) {
        return kitJobService.groupAdd(kitJobGroup);
    }

    @PostMapping("update")
    @Operation(summary = "更新执行器")
    public R<String> update(@RequestBody KitJobGroup kitJobGroup) {
        return kitJobService.groupUpdate(kitJobGroup);
    }

    @PostMapping("remove")
    @Operation(summary = "删除执行器")
    public R<String> remove(@RequestParam("id") int id) {
        return kitJobService.groupRemove(id);
    }

    @GetMapping("loadById")
    @Operation(summary = "根据ID查询执行器")
    public R<KitJobGroup> loadById(@RequestParam("id") int id) {
        return kitJobService.groupLoadById(id);
    }

}
