package org.quyq.gwsu.kit.job.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.admin.JobAdminClientApi;
import org.quyq.gwsu.common.job.openapi.admin.dto.CallbackRequest;
import org.quyq.gwsu.common.job.openapi.admin.dto.RegistryRequest;
import org.quyq.gwsu.kit.job.scheduler.openapi.impl.AdminBizImpl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务管理端开放API控制器
 */
@RestController
@RequestMapping("/job/api")
@Tag(name = "任务调度API")
@RequiredArgsConstructor
public class JobOpenApiController implements JobAdminClientApi {

    private final AdminBizImpl adminBizImpl;

    @Override
    @PostMapping("registry")
    public R<String> registry(@RequestBody RegistryRequest request) {
        return adminBizImpl.registry(request);
    }

    @Override
    @PostMapping("registryRemove")
    public R<String> registryRemove(@RequestBody RegistryRequest request) {
        return adminBizImpl.registryRemove(request);
    }

    @Override
    @PostMapping("callback")
    public R<String> callback(@RequestBody CallbackRequest request) {
        return adminBizImpl.callback(request);
    }

}
