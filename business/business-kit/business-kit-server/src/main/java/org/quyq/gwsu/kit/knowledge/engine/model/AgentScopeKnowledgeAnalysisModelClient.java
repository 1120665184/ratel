package org.quyq.gwsu.kit.knowledge.engine.model;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.Model;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.model.ModelProvider;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 基于 AgentScope 的知识源分析模型客户端。
 */
@Component
@Slf4j
@RegisterReflectionForBinding(AgentScopeKnowledgeAnalysisModelClient.KnowledgeAnalysisResponse.class)
public class AgentScopeKnowledgeAnalysisModelClient implements KnowledgeAnalysisModelClient {

    private final AgentScopeResponseParser responseParser;

    public AgentScopeKnowledgeAnalysisModelClient(AgentScopeResponseParser responseParser) {
        this.responseParser = responseParser;
    }

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
                Msg result = agent.call(messages, RuntimeContext.builder().build()).block();
                log.info("知识源分析模型原始返回: rawTextPreview={}", abbreviate(responseParser.text(result), 300));
                KnowledgeAnalysisResponse response = responseParser.parse(result, KnowledgeAnalysisResponse.class);
                if (response == null) {
                    String digest = responseParser.text(result);
                    if (!StringUtils.hasText(digest)) {
                        log.warn("知识源分析模型未返回可解析文本");
                        throw new BusinessException(KitErrorCode.E03008);
                    }
                    log.warn("知识源分析结构化解析失败，回退为纯文本: digestPreview={}", abbreviate(digest, 200));
                    response = new KnowledgeAnalysisResponse(digest);
                }
                if (!StringUtils.hasText(response.analysisDigest())) {
                    log.warn("知识源分析摘要为空");
                    throw new BusinessException(KitErrorCode.E03008);
                }
                String digest = response.analysisDigest().trim();
                if (isSuspiciousDigest(digest)) {
                    log.warn("知识源分析摘要疑似占位内容: digestPreview={}", abbreviate(digest, 120));
                    throw new BusinessException(KitErrorCode.E03008);
                }
                return digest;
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(KitErrorCode.E03008, ex);
        }
    }

    record KnowledgeAnalysisResponse(String analysisDigest) {
    }

    private boolean isSuspiciousDigest(String digest) {
        if (!StringUtils.hasText(digest)) {
            return true;
        }
        String normalized = digest.trim();
        return "test".equalsIgnoreCase(normalized)
                || (normalized.length() <= 5 && normalized.chars().allMatch(ch -> ch == '.' || ch == '。' || ch == '…'));
    }

    private String abbreviate(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...[truncated]";
    }
}
