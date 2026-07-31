package org.quyq.gwsu.common.ai.agui.resolver;


import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;

/**
 * @author Quyq
 * @date 2026/7/31
 * @description
 */
@RequiredArgsConstructor
public class MultiAgentResolver implements AgentResolver {

    private final AguiAgentRegistry registry;

    @Override
    public Agent resolveAgent(String agentId, RuntimeContext runtimeContext) {

        return registry.getAgent(agentId).orElseThrow(() -> new BusinessException("Agent not found: " + agentId));
    }
}
