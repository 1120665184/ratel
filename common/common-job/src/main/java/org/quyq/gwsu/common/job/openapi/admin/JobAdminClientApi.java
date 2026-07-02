package org.quyq.gwsu.common.job.openapi.admin;

import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.admin.dto.CallbackRequest;
import org.quyq.gwsu.common.job.openapi.admin.dto.RegistryRequest;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 任务管理端API客户端
 */
@ApiClient(value = CoreConstants.Server.KIT_NAME, note = "Job Admin API")
@HttpExchange("/job/api")
public interface JobAdminClientApi {

    /**
     * 执行器注册
     *
     * @param request 注册请求
     * @return 响应
     */
    @PostExchange("registry")
    R<String> registry(RegistryRequest request);

    /**
     * 执行器注销
     *
     * @param request 注册请求
     * @return 响应
     */
    @PostExchange("registryRemove")
    R<String> registryRemove(RegistryRequest request);

    /**
     * 任务回调
     *
     * @param request 回调请求
     * @return 响应
     */
    @PostExchange("callback")
    R<String> callback(CallbackRequest request);

}
