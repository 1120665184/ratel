package org.quyq.gwsu.kit.api.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

/**
 * 执行器查询对象
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "执行器查询对象")
public class KitJobGroupDTO extends BaseDTO {

    @Schema(description = "执行器AppName")
    private String appname;

    @Schema(description = "执行器名称")
    private String name;

}
