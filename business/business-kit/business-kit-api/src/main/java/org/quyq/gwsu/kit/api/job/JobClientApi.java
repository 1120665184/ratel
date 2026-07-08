package org.quyq.gwsu.kit.api.job;

import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.api.job.dto.JobInfoCreateDTO;
import org.quyq.gwsu.kit.api.job.fallback.JobClientApiFallbackFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 服务间任务管理 API。
 */
@ApiClient(value = CoreConstants.Server.KIT_NAME, note = "任务管理客户端API",
        fallbackFactory = JobClientApiFallbackFactory.class)
@HttpExchange("/job/info")
public interface JobClientApi {

    @PostExchange("addByDTO")
    R<String> create(@RequestBody JobInfoCreateDTO dto);

    default R<String> createAndStart(JobInfoCreateDTO dto) {
        R<String> createResult = create(dto);
        if (!createResult.isSuccess()) {
            return createResult;
        }
        if (createResult.data() == null || createResult.data().isBlank()) {
            return R.fail("创建任务成功但未返回任务ID");
        }
        R<String> startResult = start(createResult.data());
        return startResult.isSuccess() ? createResult : startResult;
    }

    @PostExchange("updateByDTO")
    R<String> update(@RequestBody JobInfoCreateDTO dto);

    @PostExchange("start")
    R<String> start(@RequestParam("id") String id);

    @PostExchange("stop")
    R<String> stop(@RequestParam("id") String id);

    @PostExchange("remove")
    R<String> remove(@RequestParam("id") String id);
}
