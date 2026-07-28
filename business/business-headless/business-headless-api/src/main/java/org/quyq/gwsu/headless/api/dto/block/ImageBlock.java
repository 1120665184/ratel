package org.quyq.gwsu.headless.api.dto.block;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.quyq.gwsu.headless.api.dto.ContentBlock;

/**
 * 图片内容块，仅支持 URL 来源
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
     * 图片 URL
     */
    private String url;

    /**
     * 媒体类型，如 "image/png"
     */
    private String mediaType;

    @Override
    public String getType() {
        return TYPE;
    }
}
