package org.quyq.gwsu.security.brain.push;


import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.RunAgentInput;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.agui.push.AguiEventPusher;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Quyq
 * @date 2026/6/15
 * @description
 */
@RequiredArgsConstructor
public class AguiEventRedisPusher implements AguiEventPusher {

    public static final String BRAIN_SSE_EVENT_CHANNEL_PREFIX = "brain_sse_event_channel_";

    private final CacheUtils cacheUtils;

    private final ObjectMapper objectMapper;


    @Override
    public void push(RunAgentInput param, AguiEvent event) {

        String threadId = param.getThreadId();
        if (StringUtils.hasText(threadId)) {
            cacheUtils.withRebel(() -> cacheUtils.convertAndSend(BRAIN_SSE_EVENT_CHANNEL_PREFIX + threadId,
                    objectMapper.writeValueAsString(event)));
        }

    }
}
