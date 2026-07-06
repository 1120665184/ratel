package org.quyq.gwsu.kit.job.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.quyq.gwsu.kit.job.scheduler.thread.HandlerRegistryInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler注册信息控制器
 */
@RestController
@RequestMapping("/job/handler")
@Tag(name = "Handler注册信息")
@RequiredArgsConstructor
public class HandlerRegistryController {

    @GetMapping("list")
    @Operation(summary = "查询所有在线Handler注册信息")
    public R<List<Map<String, Object>>> list() {
        Map<String, HandlerRegistryInfo> allHandlerRegistry = JobAdminBootstrap.getInstance().getJobRegistryHelper().getAllHandlerRegistry();
        List<Map<String, Object>> result = new ArrayList<>();
        if (allHandlerRegistry != null) {
            for (Map.Entry<String, HandlerRegistryInfo> entry : allHandlerRegistry.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("handlerName", entry.getValue().handlerName());
                item.put("appname", entry.getValue().appname());
                item.put("addresses", entry.getValue().addresses());
                item.put("conflict", JobAdminBootstrap.getInstance().getJobRegistryHelper().isConflict(entry.getKey()));
                result.add(item);
            }
        }
        return R.ok(result);
    }

    @GetMapping("load")
    @Operation(summary = "根据handler名称查询注册信息")
    public R<HandlerRegistryInfo> load(@RequestParam("handlerName") String handlerName) {
        HandlerRegistryInfo info = JobAdminBootstrap.getInstance().getJobRegistryHelper().loadByHandlerName(handlerName);
        if (info == null) {
            return R.fail("handler未注册或已下线");
        }
        return R.ok(info);
    }

}
