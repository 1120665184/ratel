package org.quyq.gwsu.security.brain.service.tool.web;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.state.AgentStateStore;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 管理 Web 页面审批索引的会话级持久化状态。
 */
@Service
@RequiredArgsConstructor
public class WebPageApprovalStateService {

    private final AgentStateStore agentStateStore;

    public void save(RuntimeContext runtimeContext, Set<Integer> approvalIndexes) {
        SessionKey sessionKey = resolveSessionKey(runtimeContext);
        if (sessionKey == null) {
            return;
        }
        List<Integer> indexes = CollectionUtils.isEmpty(approvalIndexes)
                ? List.of()
                : approvalIndexes.stream().distinct().toList();
        agentStateStore.save(sessionKey.userId(), sessionKey.sessionId(),
                AIConstants.AgentStateKey.WEB_PAGE_APPROVAL_INDEXES,
                new WebPageApprovalIndexesState(indexes));
    }

    public Set<Integer> load(RuntimeContext runtimeContext) {
        SessionKey sessionKey = resolveSessionKey(runtimeContext);
        if (sessionKey == null) {
            return null;
        }
        return agentStateStore.get(sessionKey.userId(), sessionKey.sessionId(),
                        AIConstants.AgentStateKey.WEB_PAGE_APPROVAL_INDEXES,
                        WebPageApprovalIndexesState.class)
                .map(WebPageApprovalIndexesState::indexes)
                .map(this::toIndexes)
                .orElse(null);
    }

    public void clear(RuntimeContext runtimeContext) {
        SessionKey sessionKey = resolveSessionKey(runtimeContext);
        if (sessionKey == null) {
            return;
        }
        agentStateStore.delete(sessionKey.userId(), sessionKey.sessionId(),
                AIConstants.AgentStateKey.WEB_PAGE_APPROVAL_INDEXES);
    }

    private Set<Integer> toIndexes(List<Integer> indexes) {
        if (CollectionUtils.isEmpty(indexes)) {
            return Collections.emptySet();
        }
        LinkedHashSet<Integer> result = new LinkedHashSet<>();
        for (Integer index : indexes) {
            if (index != null) {
                result.add(index);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private SessionKey resolveSessionKey(RuntimeContext runtimeContext) {
        if (runtimeContext == null
                || !StringUtils.hasText(runtimeContext.getSessionId())) {
            return null;
        }
        return new SessionKey(runtimeContext.getUserId(), runtimeContext.getSessionId());
    }

    private record SessionKey(String userId, String sessionId) {
    }
}
