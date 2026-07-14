package org.quyq.gwsu.common.authentication.login.impl.apikey;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.authentication.domain.AbstractLoginDTO;

/**
 * API_KEY 登录参数
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "API_KEY 登录参数")
public class ApiKeyLoginDTO extends AbstractLoginDTO {

    @Schema(description = "API_KEY")
    private String apiKey;

    @Schema(description = "客户端IP")
    private String ip;
}
