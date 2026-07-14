package org.quyq.gwsu.system.api.apikey.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

import java.time.LocalDateTime;

/**
 * API_KEY 列表 VO
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "API_KEY 列表信息")
public class ApiKeyVO extends BaseVO {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String apiKeyName;

    @Schema(description = "脱敏后的 Key")
    private String maskedKey;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "最近使用时间")
    private LocalDateTime lastUsedTime;

    @Schema(description = "备注")
    private String remark;
}
