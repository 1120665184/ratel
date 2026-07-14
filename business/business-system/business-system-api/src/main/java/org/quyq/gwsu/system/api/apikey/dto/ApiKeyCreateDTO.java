package org.quyq.gwsu.system.api.apikey.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.system.api.apikey.enums.ApiKeyExpireTypeEnum;

import java.time.LocalDateTime;

/**
 * API_KEY 创建参数
 *
 * @author Quyq
 */
@Data
@Schema(description = "API_KEY 创建参数")
public class ApiKeyCreateDTO {

    @Schema(description = "名称")
    private String apiKeyName;

    @Schema(description = "有效期类型")
    private ApiKeyExpireTypeEnum expireType;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "有效天数")
    private Integer expireDays;

    @Schema(description = "备注")
    private String remark;
}
