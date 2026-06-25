package org.quyq.gwsu.headless.api.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 消息内容块基类，模仿 io.agentscope.core.message.ContentBlock
 * <p>
 * 使用 Jackson 多态序列化，根据 "type" 字段自动选择子类反序列化
 *
 * @author Quyq
 * @date 2026/6/25
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextBlock.class, name = TextBlock.TYPE),
        @JsonSubTypes.Type(value = ImageBlock.class, name = ImageBlock.TYPE),
        @JsonSubTypes.Type(value = VideoBlock.class, name = VideoBlock.TYPE),
        @JsonSubTypes.Type(value = AudioBlock.class, name = AudioBlock.TYPE),
        @JsonSubTypes.Type(value = ThinkingBlock.class, name = ThinkingBlock.TYPE),
        @JsonSubTypes.Type(value = ToolUseBlock.class, name = ToolUseBlock.TYPE),
        @JsonSubTypes.Type(value = ToolResultBlock.class, name = ToolResultBlock.TYPE)
})
public abstract class ContentBlock {

    /**
     * 内容块类型标识，子类必须返回常量
     */
    public abstract String getType();

}
