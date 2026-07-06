package org.quyq.gwsu.kit.api.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 任务日志查询对象
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "任务日志查询对象")
public class KitJobLogDTO extends BaseDTO {

    @Schema(description = "任务ID")
    private String jobId;

    @Schema(description = "日志状态：1-成功，2-失败，3-运行中")
    private Integer logStatus;

    @Schema(description = "触发时间起始")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime triggerTimeStart;

    @Schema(description = "触发时间截止")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime triggerTimeEnd;

}
