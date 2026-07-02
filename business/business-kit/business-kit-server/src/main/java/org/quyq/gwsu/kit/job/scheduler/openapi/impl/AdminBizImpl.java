package org.quyq.gwsu.kit.job.scheduler.openapi.impl;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.admin.dto.CallbackRequest;
import org.quyq.gwsu.common.job.openapi.admin.dto.RegistryRequest;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.springframework.stereotype.Service;

/**
 * Admin业务实现
 */
@Service
public class AdminBizImpl {

    /**
     * 回调
     */
    public R<String> callback(CallbackRequest callbackRequest) {
        return JobAdminBootstrap.getInstance().getJobCompleteHelper().callback(callbackRequest.getCallbackList());
    }

    /**
     * 注册
     */
    public R<String> registry(RegistryRequest registryRequest) {
        return JobAdminBootstrap.getInstance().getJobRegistryHelper().registry(registryRequest);
    }

    /**
     * 注销注册
     */
    public R<String> registryRemove(RegistryRequest registryRequest) {
        return JobAdminBootstrap.getInstance().getJobRegistryHelper().registryRemove(registryRequest);
    }

}
