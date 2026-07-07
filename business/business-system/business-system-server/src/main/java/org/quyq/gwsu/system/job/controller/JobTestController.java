package org.quyq.gwsu.system.job.controller;

import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.domain.BaseDTO;
import org.quyq.gwsu.common.core.domain.R;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * URL调用模式定时任务测试控制器
 * <p>
 * 供 UrlJobHandler 调用测试使用，模拟实际业务接口
 *
 * @author Quyq
 */
@Slf4j
@RestController
@RequestMapping("job/test")
@Tag(name = "定时任务测试")
@RequiredArgsConstructor
public class JobTestController {

    private final Gson gson ;

    @PostMapping("callback")
    @Operation(summary = "URL调用模式-回调测试")
    public R<Map<String, Object>> callback(@RequestBody JobTestDTO dto) {
        log.info("收到URL调用模式定时任务回调: {}", dto);
        BaseDTO.JobParams jobParams = dto.getJobParams();

        Map<String, Object> result = Map.of();
        if (jobParams != null) {
            result = Map.of(
                    "message", "回调测试成功",
                    "receiveName", dto.getName(),
                    "receiveTime", LocalDateTime.now().toString(),
                    "jobShardIndex", jobParams.jobShardIndex(),
                    "jobShardTotal", jobParams.jobShardTotal(),
                    "xxlJobId", jobParams.xxlJobId(),
                    "instanceId" , jobParams.xxlJobInstanceId()
            );
        }
        log.info("result: {}", gson.toJson(result));
        return R.ok(result);
    }

    @PostMapping("simple")
    @Operation(summary = "URL调用模式-简单测试")
    public R<String> simple(@RequestBody BaseDTO baseDTO) {
        log.info("收到简单定时任务调用, jobParams={}", baseDTO.getJobParams());
        return R.ok("简单测试成功，任务ID: " +
                (baseDTO.getJobParams() != null ? baseDTO.getJobParams().xxlJobId() : "unknown"));
    }

    /**
     * 测试用 DTO
     */
    @lombok.Data
    @lombok.EqualsAndHashCode(callSuper = true)
    public static class JobTestDTO extends BaseDTO {
        private String name;
        private String description;
    }
}
