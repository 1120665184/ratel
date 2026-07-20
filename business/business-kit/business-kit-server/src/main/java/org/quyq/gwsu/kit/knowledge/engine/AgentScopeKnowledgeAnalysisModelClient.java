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
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 基于 AgentScope 的知识源分析模型客户端。
 */
@Component
@RegisterReflectionForBinding(AgentScopeKnowledgeAnalysisModelClient.KnowledgeAnalysisResponse.class)
public class AgentScopeKnowledgeAnalysisModelClient implements KnowledgeAnalysisModelClient {

    private static final String SYSTEM_PROMPT = """
            你是知识库导入阶段的分析助手。
            你的任务是提炼事实、结构和约束，不得添加原文未表达的事实。
            输出必须只包含 analysisDigest 字段。
            """;

    @Override
    public String analyzeChunk(String prompt) {
        return invoke(prompt);
    }

    @Override
    public String summarizeDigests(String prompt) {
        return invoke(prompt);
    }

    private String invoke(String prompt) {
        try {
            Model model = ModelProvider.generateModel();
            try (ReActAgent agent = ReActAgent.builder()
                    .name("knowledgeAnalysisModel")
                    .sysPrompt(SYSTEM_PROMPT)
                    .model(model)
                    .build()) {
                List<Msg> messages = List.of(Msg.builder()
                        .role(MsgRole.USER)
                        .textContent(prompt)
                        .build());
                KnowledgeAnalysisResponse response = Optional.ofNullable(
                                agent.call(messages, KnowledgeAnalysisResponse.class, RuntimeContext.builder().build())
                                        .block())
                        .map(result -> result.getStructuredData(KnowledgeAnalysisResponse.class))
                        .orElseThrow(() -> new BusinessException(KitErrorCode.E03008));
                if (!StringUtils.hasText(response.analysisDigest())) {
                    throw new BusinessException(KitErrorCode.E03008);
                }
                return response.analysisDigest();
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(KitErrorCode.E03008, ex);
        }
    }

    record KnowledgeAnalysisResponse(String analysisDigest) {
    }
}
