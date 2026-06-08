package org.quyq.gwsu.security.api.tablemodel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "业务功能查询条件")
public class BusinessFunctionQueryDTO extends BaseDTO {

    @Schema(description = "业务名称（模糊查询）")
    private String name;
}
