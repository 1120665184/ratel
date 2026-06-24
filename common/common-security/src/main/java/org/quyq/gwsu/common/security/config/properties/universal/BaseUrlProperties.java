package org.quyq.gwsu.common.security.config.properties.universal;


import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.StringUtils;

/**
 * @author Quyq
 * @date 2026/6/24
 * @description 通用的基础配置 , 对应： 设置 -> 通用配置 -> 基础配置
 */
public record BaseUrlProperties(
        @Schema(description = "前端地址")
        String viewBaseUrl,

        @Schema(description = "后端API地址")
        String apiBaseUrl
) {
    public final static String CONFIG_KEY = "basic_url_config";

    @Override
    public String viewBaseUrl() {
        if (StringUtils.hasText(viewBaseUrl) && viewBaseUrl.endsWith("/")) {
            return viewBaseUrl.substring(0, viewBaseUrl.length() - 1);
        }
        return viewBaseUrl;
    }

    @Override
    public String apiBaseUrl() {
        if (StringUtils.hasText(apiBaseUrl) && apiBaseUrl.endsWith("/")) {
            return apiBaseUrl.substring(0, apiBaseUrl.length() - 1);
        }
        return apiBaseUrl;
    }
}
