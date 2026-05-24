package org.quyq.gwsu.common.ai.agui;


import io.agentscope.core.agui.AguiException;
import org.quyq.gwsu.common.ai.agui.domain.AIRunnerInstanceWrapper;
import org.quyq.gwsu.common.core.utils.ServletUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Quyq
 * @date 2026/5/22
 * @description 统一管理智能体emitter连接
 */
public class EmitterWrapperManager {

    private final Map<String, AIRunnerInstanceWrapper> glabEmitter = new ConcurrentHashMap<>();


    public void put(String thread, AIRunnerInstanceWrapper instanceWrapper) {
        glabEmitter.put(thread, instanceWrapper);
    }


    public void remove(String thread) {
        glabEmitter.remove(thread);
    }


    public AIRunnerInstanceWrapper get() {
        String threadId = ServletUtils.getHeaders().get("thread-id");
        if (Objects.isNull(threadId)) {
            throw new AguiException("thread-id is null");
        }
        return glabEmitter.get(threadId);
    }

    public AIRunnerInstanceWrapper get(String thread) {
        return glabEmitter.get(thread);
    }


}
