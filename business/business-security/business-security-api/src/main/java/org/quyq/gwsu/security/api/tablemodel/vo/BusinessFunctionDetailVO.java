package org.quyq.gwsu.security.api.tablemodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "业务功能详情")
public class BusinessFunctionDetailVO extends BusinessFunctionVO {

    @Schema(description = "关联的表模型列表")
    private List<TableModelTableVO> tables;
}
