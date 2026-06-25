package org.quyq.gwsu.headless.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 文本内容块
 *
 * @author Quyq
 * @date 2026/6/25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextBlock extends ContentBlock {

    public static final String TYPE = "text";

    private String text;

    @Override
    public String getType() {
        return TYPE;
    }
}
