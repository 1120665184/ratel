package org.quyq.gwsu.headless.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具调用内容块，记录模型发起的工具调用请求
 *
 * @author Quyq
 * @date 2026/6/25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolUseBlock extends ContentBlock {

    public static final String TYPE = "tool_use";

    /**
     * 工具调用 ID
     */
    private String id;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具调用输入参数
     */
    private Map<String, Object> input;

    /**
     * 工具调用内容
     */
    private String content;

    /**
     * 工具调用相关元数据
     */
    private Map<String, Object> metadata;

    @Override
    public String getType() {
        return TYPE;
    }
}
