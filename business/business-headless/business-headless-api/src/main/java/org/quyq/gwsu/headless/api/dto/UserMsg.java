package org.quyq.gwsu.headless.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quyq.gwsu.headless.api.dto.block.TextBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户消息，模仿 io.agentscope.core.message.Msg 的 USER 角色
 * <p>
 * 替代 Spring AI 的 UserMessage，支持多模态内容块
 *
 * @author Quyq
 * @date 2026/6/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMsg {

    /**
     * 消息内容块列表
     */
    private List<ContentBlock> content;

    /**
     * 提取所有文本内容拼接为字符串
     */
    public String getTextContent() {
        if (content == null || content.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : content) {
            if (block instanceof TextBlock textBlock && textBlock.getText() != null) {
                sb.append(textBlock.getText());
            }
        }
        return sb.toString();
    }

    /**
     * 获取指定类型的内容块列表
     */
    public <T extends ContentBlock> List<T> getContentBlocks(Class<T> clazz) {
        if (content == null) {
            return List.of();
        }
        List<T> result = new ArrayList<>();
        for (ContentBlock block : content) {
            if (clazz.isInstance(block)) {
                result.add(clazz.cast(block));
            }
        }
        return result;
    }

    /**
     * 是否包含指定类型的内容块
     */
    public <T extends ContentBlock> boolean hasContentBlocks(Class<T> clazz) {
        if (content == null) {
            return false;
        }
        return content.stream().anyMatch(clazz::isInstance);
    }

    /**
     * 便捷构造：纯文本消息
     */
    public static UserMsg ofText(String text) {
        return UserMsg.builder()
                .content(List.of(TextBlock.builder().text(text).build()))
                .build();
    }
}
