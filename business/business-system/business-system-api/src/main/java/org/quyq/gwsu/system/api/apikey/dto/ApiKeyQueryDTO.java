package org.quyq.gwsu.system.api.apikey.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

/**
 * API_KEY 查询参数
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "API_KEY 查询参数")
public class ApiKeyQueryDTO extends BaseDTO {

    @Schema(description = "名称")
    private String apiKeyName;
}
