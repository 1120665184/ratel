package org.quyq.gwsu.security.brain.push;


import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.agui.event.AguiEvent;
import org.quyq.gwsu.common.ai.agui.model.RunAgentInput;
import org.quyq.gwsu.common.ai.agui.push.AguiEventPusher;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * @author Quyq
 * @date 2026/6/15
 * @description 基于Redis List的SSE事件推送器，使用LPUSH写入事件，消费端通过RPOP按序读取
 */
@RequiredArgsConstructor
public class AguiEventRedisPusher implements AguiEventPusher {

    public static final String BRAIN_SSE_EVENT_LIST_PREFIX = "brain_sse_event_list:";

    private static final Duration LIST_TTL = Duration.ofHours(1);

    private final CacheUtils cacheUtils;

    private final ObjectMapper objectMapper;


    @Override
    public void push(RunAgentInput param, AguiEvent event) {

        String threadId = param.threadId();
        if (StringUtils.hasText(threadId)) {
            String key = BRAIN_SSE_EVENT_LIST_PREFIX + threadId;
            cacheUtils.withRebel(() -> {
                cacheUtils.lPush(key, objectMapper.writeValueAsString(event));
                cacheUtils.expire(key, LIST_TTL);
                return null;
            });
        }

    }
}
