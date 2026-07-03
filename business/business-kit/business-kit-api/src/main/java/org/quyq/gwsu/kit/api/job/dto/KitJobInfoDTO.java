package org.quyq.gwsu.kit.api.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

/**
 * 任务信息查询对象
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "任务信息查询对象")
public class KitJobInfoDTO extends BaseDTO {

    @Schema(description = "执行器ID")
    private String jobGroup;

    @Schema(description = "调度状态：0-停止，1-运行，-1-全部")
    private Integer triggerStatus;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "任务Handler")
    private String executorHandler;

    @Schema(description = "负责人")
    private String author;

}
