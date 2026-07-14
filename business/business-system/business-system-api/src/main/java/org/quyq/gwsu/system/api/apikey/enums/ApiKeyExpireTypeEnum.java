package org.quyq.gwsu.system.api.apikey.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API_KEY 有效期类型
 *
 * @author Quyq
 */
@Schema(description = "API_KEY 有效期类型")
public enum ApiKeyExpireTypeEnum {
    FOREVER,
    CUSTOM_DATE,
    AFTER_DAYS
}
