package org.quyq.gwsu.headless.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 图片内容块，支持 URL 或 Base64 两种来源
 *
 * @author Quyq
 * @date 2026/6/25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageBlock extends ContentBlock {

    public static final String TYPE = "image";

    /**
     * 图片 URL，与 base64 二选一
     */
    private String url;

    /**
     * Base64 媒体类型，如 "image/png"，base64 模式时使用
     */
    private String mediaType;

    /**
     * Base64 编码数据，base64 模式时使用
     */
    private String data;

    @Override
    public String getType() {
        return TYPE;
    }
}
