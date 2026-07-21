package org.quyq.gwsu.kit.knowledge.engine;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.Model;
import org.quyq.gwsu.common.ai.model.ModelProvider;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 基于 AgentScope 的知识 Page 匹配与合并模型客户端。
 */
@Component
@RegisterReflectionForBinding({
        KnowledgePageMatchAction.class,
        KnowledgePageMatchDecision.class,
        KnowledgePageMergePlan.class,
        KnowledgePageMergePlan.Item.class})
public class AgentScopeKnowledgePageMergeModelClient implements KnowledgePageMergeModelClient {

    private static final String MATCH_SYSTEM_PROMPT = """
            你是知识库 Wiki Page 归属判断助手。
            只根据用户提供的候选 Page 判断，不得编造候选。
            输出必须严格符合结构化字段定义。
            """;

    private static final String MERGE_SYSTEM_PROMPT = """
            你是知识库 Wiki Page 结构合并助手。
            你只能生成合并计划，不得直接改写正文事实。
            输出必须严格符合结构化字段定义。
            """;

    @Override
    public KnowledgePageMatchDecision matchPage(String prompt, List<KnowledgePageCandidate> candidates) {
        if (CollectionUtils.isEmpty(candidates)) {
            return new KnowledgePageMatchDecision(KnowledgePageMatchAction.CREATE_NEW_PAGE, "", 1.0D, "没有候选 Page");
        }
        KnowledgePageMatchDecision decision = invoke(prompt, KnowledgePageMatchDecision.class, MATCH_SYSTEM_PROMPT, "knowledgePageMatcher");
        if (decision == null || decision.action() == null) {
            throw new BusinessException(KitErrorCode.E03008);
        }
        if (decision.matchedExistingPage()
                && candidates.stream().noneMatch(candidate -> candidate.pageId().equals(decision.pageId()))) {
            return new KnowledgePageMatchDecision(KnowledgePageMatchAction.CREATE_NEW_PAGE, "", 0.0D, "模型返回了非候选 Page");
        }
        return decision;
    }

    @Override
    public KnowledgePageMergePlan planMerge(String prompt) {
        KnowledgePageMergePlan plan = invoke(prompt, KnowledgePageMergePlan.class, MERGE_SYSTEM_PROMPT, "knowledgePageMergePlanner");
        if (plan == null || !StringUtils.hasText(plan.title()) || CollectionUtils.isEmpty(plan.items())) {
            throw new BusinessException(KitErrorCode.E03008);
        }
        return plan;
    }

    private <T> T invoke(String prompt, Class<T> responseType, String systemPrompt, String agentName) {
        try {
            Model model = ModelProvider.generateModel();
            try (ReActAgent agent = ReActAgent.builder()
                    .name(agentName)
                    .sysPrompt(systemPrompt)
                    .model(model)
                    .build()) {
                List<Msg> messages = List.of(Msg.builder()
                        .role(MsgRole.USER)
                        .textContent(prompt)
                        .build());
                return Optional.ofNullable(agent.call(messages, responseType, RuntimeContext.builder().build()).block())
                        .map(result -> result.getStructuredData(responseType))
                        .orElseThrow(() -> new BusinessException(KitErrorCode.E03008));
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(KitErrorCode.E03008, ex);
        }
    }
}
