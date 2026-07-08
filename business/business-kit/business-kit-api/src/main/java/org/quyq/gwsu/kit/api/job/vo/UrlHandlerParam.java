package org.quyq.gwsu.kit.api.job.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * URL调用模式定时任务参数
 *
 * @author Quyq
 */
@Schema(description = "URL调用模式定时任务参数")
public record UrlHandlerParam(

        @Schema(description = "模块前缀（如 kit、security、system），对应 BusinessModuleInfoProvider 配置的 prefix")
        String prefix,

        @Schema(description = "接口路径，必须以 / 开头（如 /job/api/callback）")
        String url,

        @Schema(description = "请求体 JSON 字符串，可选。存储时为嵌套 JSON，执行时需二次解析")
        String bodyJson
) {}
