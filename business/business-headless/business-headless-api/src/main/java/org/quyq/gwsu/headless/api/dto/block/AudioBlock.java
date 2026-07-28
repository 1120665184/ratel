package org.quyq.gwsu.headless.api.dto.block;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.quyq.gwsu.headless.api.dto.ContentBlock;

/**
 * 音频内容块，仅支持 URL 来源
 *
 * @author Quyq
 * @date 2026/6/25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioBlock extends ContentBlock {

    public static final String TYPE = "audio";

    /**
     * 音频 URL
     */
    private String url;

    /**
     * 媒体类型，如 "audio/mp3"
     */
    private String mediaType;

    @Override
    public String getType() {
        return TYPE;
    }
}
