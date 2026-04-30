package org.quyq.gwsu.security.brain.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.agentscope.core.agui.converter.AguiMessageConverter;
import io.agentscope.core.agui.model.AguiMessage;
import io.agentscope.core.message.Msg;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.security.api.brain.dto.BrainHistoryQueryDTO;
import org.quyq.gwsu.security.api.brain.vo.BrainHistorySessionVo;
import org.quyq.gwsu.security.brain.mapper.BrainHistoryMapper;
import org.quyq.gwsu.security.brain.service.IBrainHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 大脑历史会话服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class BrainHistoryServiceImpl implements IBrainHistoryService {

    private final BrainHistoryMapper brainHistoryMapper;

    private final ObjectMapper objectMapper;

    private final AguiMessageConverter messageConverter = new AguiMessageConverter();

    @Override
    public IPage<BrainHistorySessionVo> pageHistorySessions(BrainHistoryQueryDTO query, String userId) {
        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 10;
        int offset = (pageNum - 1) * pageSize;

        // 查询列表
        List<BrainHistorySessionVo> records = brainHistoryMapper.selectHistorySessions(userId, offset, pageSize);

        // 查询总数
        Long total = brainHistoryMapper.countHistorySessions(userId);

        // 处理标题和时间显示
        for (BrainHistorySessionVo vo : records) {
            // 从 state_data JSON 中提取第一条消息内容作为标题
            vo.setTitle(extractTitle(vo.getTitle()));
            // 计算时间显示
            vo.setTimeDisplay(formatTimeDisplay(vo.getUpdatedAt()));
        }

        // 构建分页结果
        Page<BrainHistorySessionVo> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(records);
        return page;
    }

    @Override
    public List<AguiMessage> getSessionMessages(String sessionId, String userId) {
        List<String> messages = brainHistoryMapper.selectSessionMessages(sessionId, userId);
        if (CollectionUtils.isEmpty(messages)) {
            return Collections.emptyList();
        }

        return messages.stream().map(mess -> {
            Msg msg = objectMapper.readValue(mess, Msg.class);
            return messageConverter.toAguiMessage(msg);
        }).toList();

    }

    @Override
    public Boolean deleteSession(String sessionId, String userId) {
        return brainHistoryMapper.deleteSession(sessionId, userId) > 0;
    }

    /**
     * 从 state_data JSON 中提取标题
     * state_data 格式: {"id":"xxx","role":"USER","content":[{"type":"text","text":"哈哈"}],...}
     */
    private String extractTitle(String stateData) {
        if (CharSequenceUtil.isBlank(stateData)) {
            return "新对话";
        }
        try {
            // 简单提取 content 中的 text 字段
            int textIndex = stateData.indexOf("\"text\":\"");
            if (textIndex > 0) {
                int start = textIndex + 8;
                int end = stateData.indexOf("\"", start);
                if (end > start) {
                    String text = stateData.substring(start, end);
                    // 截取前50字符
                    return text.length() > 50 ? text.substring(0, 50) + "..." : text;
                }
            }
        } catch (Exception ignored) {
        }
        return "新对话";
    }

    /**
     * 格式化时间显示
     */
    private String formatTimeDisplay(LocalDateTime updatedAt) {
        if (updatedAt == null) {
            return "";
        }

        Duration duration = Duration.between(updatedAt, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        } else if (hours < 24) {
            return hours + "小时前";
        } else if (days < 30) {
            return days + "天前";
        } else {
            long months = days / 30;
            return months + "个月前";
        }
    }
}
