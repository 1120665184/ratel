package org.quyq.gwsu.system.api.apikey.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

import java.time.LocalDateTime;

/**
 * API_KEY 创建结果
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "API_KEY 创建结果")
public class ApiKeyCreateResultVO extends BaseVO {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String apiKeyName;

    @Schema(description = "完整 API_KEY，仅创建时返回")
    private String apiKey;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "备注")
    private String remark;
}
