package org.quyq.gwsu.headless.api.dto.block;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.quyq.gwsu.headless.api.dto.ContentBlock;

import java.util.Map;

/**
 * 思考内容块，记录模型推理过程中的思考内容
 *
 * @author Quyq
 * @date 2026/6/25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThinkingBlock extends ContentBlock {

    public static final String TYPE = "thinking";

    /**
     * 思考内容
     */
    private String thinking;

    /**
     * 推理相关元数据
     */
    private Map<String, Object> metadata;

    @Override
    public String getType() {
        return TYPE;
    }
}
