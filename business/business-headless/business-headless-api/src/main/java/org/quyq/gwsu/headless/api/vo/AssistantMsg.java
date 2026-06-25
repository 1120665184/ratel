package org.quyq.gwsu.headless.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quyq.gwsu.headless.api.dto.AudioBlock;
import org.quyq.gwsu.headless.api.dto.ContentBlock;
import org.quyq.gwsu.headless.api.dto.ImageBlock;
import org.quyq.gwsu.headless.api.dto.TextBlock;
import org.quyq.gwsu.headless.api.dto.ThinkingBlock;
import org.quyq.gwsu.headless.api.dto.ToolResultBlock;
import org.quyq.gwsu.headless.api.dto.ToolUseBlock;
import org.quyq.gwsu.headless.api.dto.VideoBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 助手消息，模仿 io.agentscope.core.message.Msg 的 ASSISTANT 角色
 * <p>
 * 替代 Spring AI 的 AssistantMessage，支持多模态内容块和元数据
 *
 * @author Quyq
 * @date 2026/6/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantMsg {

    /**
     * 消息内容块列表
     */
    private List<ContentBlock> content;

    /**
     * 消息元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

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
     * 便捷方法：获取所有图片块
     */
    public List<ImageBlock> getImageBlocks() {
        return getContentBlocks(ImageBlock.class);
    }

    /**
     * 便捷方法：获取所有视频块
     */
    public List<VideoBlock> getVideoBlocks() {
        return getContentBlocks(VideoBlock.class);
    }

    /**
     * 便捷方法：获取所有音频块
     */
    public List<AudioBlock> getAudioBlocks() {
        return getContentBlocks(AudioBlock.class);
    }

    /**
     * 便捷方法：获取所有思考块
     */
    public List<ThinkingBlock> getThinkingBlocks() {
        return getContentBlocks(ThinkingBlock.class);
    }

    /**
     * 便捷方法：获取所有工具调用块
     */
    public List<ToolUseBlock> getToolUseBlocks() {
        return getContentBlocks(ToolUseBlock.class);
    }

    /**
     * 便捷方法：获取所有工具返回块
     */
    public List<ToolResultBlock> getToolResultBlocks() {
        return getContentBlocks(ToolResultBlock.class);
    }

    /**
     * 便捷构造：纯文本消息
     */
    public static AssistantMsg ofText(String text) {
        return AssistantMsg.builder()
                .content(List.of(TextBlock.builder().text(text).build()))
                .build();
    }

    /**
     * 便捷构造：空消息
     */
    public static AssistantMsg empty() {
        return AssistantMsg.builder()
                .content(List.of())
                .build();
    }
}
