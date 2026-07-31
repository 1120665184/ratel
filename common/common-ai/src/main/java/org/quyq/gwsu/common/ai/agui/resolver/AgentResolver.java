package org.quyq.gwsu.common.ai.agui.resolver;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;

public interface AgentResolver {

    Agent resolveAgent(String agentId, RuntimeContext runtimeContext);

}
