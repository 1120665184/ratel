package org.quyq.gwsu.headless.api.dto.block;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.quyq.gwsu.headless.api.dto.ContentBlock;

import java.util.List;
import java.util.Map;

/**
 * 工具返回内容块，记录工具调用的返回结果
 *
 * @author Quyq
 * @date 2026/6/25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResultBlock extends ContentBlock {

    public static final String TYPE = "tool_result";

    /**
     * 对应的工具调用 ID
     */
    private String id;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具返回内容
     */
    private List<ContentBlock> output;

    /**
     * 工具返回相关元数据
     */
    private Map<String, Object> metadata;

    /**
     * 工具是否被挂起（如需人工审批等场景）
     */
    private Boolean suspended;

    @Override
    public String getType() {
        return TYPE;
    }
}
