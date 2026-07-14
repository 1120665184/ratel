package org.quyq.gwsu.common.security.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * API_KEY 登录请求
 *
 * @author Quyq
 */
@Data
@Schema(description = "API_KEY 登录请求")
public class ApiKeyLoginRequest {

    @Schema(description = "API_KEY")
    private String apiKey;

    @Schema(description = "客户端IP")
    private String ip;
}
