package org.quyq.gwsu.security.brain.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.agentscope.core.agui.converter.AguiMessageConverter;
import io.agentscope.core.agui.model.AguiMessage;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.state.AgentState;
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
import java.util.Optional;

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
            AgentState agentState = parseAgentState(vo.getTitle()).orElse(null);
            List<Msg> context = agentState != null ? agentState.getContext() : Collections.emptyList();
            vo.setTitle(extractTitle(context));
            vo.setMessageCount(context.size());
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

        return parseAgentState(messages.getFirst())
                .map(AgentState::getContext)
                .orElse(Collections.emptyList())
                .stream()
                .map(messageConverter::toAguiMessage)
                .toList();

    }

    @Override
    public Boolean deleteSession(String sessionId, String userId) {
        return brainHistoryMapper.deleteSession(sessionId, userId) > 0;
    }

    /**
     * 从 AgentState 上下文中提取首条用户文本作为标题。
     */
    private String extractTitle(List<Msg> context) {
        if (CollectionUtils.isEmpty(context)) {
            return "新对话";
        }

        for (Msg msg : context) {
            if (msg == null || msg.getRole() != MsgRole.USER) {
                continue;
            }
            String text = extractText(msg);
            if (CharSequenceUtil.isNotBlank(text)) {
                return truncateTitle(text);
            }
        }
        return "新对话";
    }

    private Optional<AgentState> parseAgentState(String stateData) {
        if (CharSequenceUtil.isBlank(stateData)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(stateData, AgentState.class));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String extractText(Msg msg) {
        if (msg == null || CollectionUtils.isEmpty(msg.getContent())) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (ContentBlock contentBlock : msg.getContent()) {
            if (contentBlock instanceof TextBlock textBlock && CharSequenceUtil.isNotBlank(textBlock.getText())) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(textBlock.getText());
            }
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private String truncateTitle(String text) {
        return text.length() > 50 ? text.substring(0, 50) + "..." : text;
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
