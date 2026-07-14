package org.quyq.gwsu.system.api.apikey.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API_KEY 详情 VO
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "API_KEY 详情")
public class ApiKeyDetailVO extends ApiKeyVO {

    @Schema(description = "最近使用IP")
    private String lastUsedIp;
}
