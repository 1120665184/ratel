package org.quyq.gwsu.kit.knowledge.engine.page;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.Model;
import org.quyq.gwsu.common.ai.model.ModelProvider;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.engine.model.AgentScopeResponseParser;
import org.quyq.gwsu.kit.knowledge.engine.support.KnowledgeGenerationPromptBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 基于 AgentScope 的知识 Page 生成器。
 */
@Component
@RegisterReflectionForBinding(AgentScopeKnowledgePageGenerator.KnowledgePageGenerationResponse.class)
public class AgentScopeKnowledgePageGenerator implements KnowledgePageGenerator {

    private final KnowledgeGenerationPromptBuilder promptBuilder;

    private final AgentScopeResponseParser responseParser;

    @Autowired
    public AgentScopeKnowledgePageGenerator(KnowledgeGenerationPromptBuilder promptBuilder,
                                            AgentScopeResponseParser responseParser) {
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
    }

    private static final String SYSTEM_PROMPT = """
            你是知识库 Wiki 页面整理助手。
            请把源文档内容整理为一个结构清晰的 Markdown 页面。
            输出必须包含 title 和 markdownContent 两个字段。
            不要添加源文档没有表达的事实。
            """;

    @Override
    public GeneratedKnowledgePage generate(KnowledgePageGenerationRequest request) {
        if (!StringUtils.hasText(request.boundedSourceText())) {
            throw new BusinessException(KitErrorCode.E03006);
        }
        try {
            Model model = ModelProvider.generateModel();
            try (ReActAgent agent = ReActAgent.builder()
                .name("knowledgePageGenerator")
                .sysPrompt(SYSTEM_PROMPT)
                .model(model)
                .build()) {
                List<Msg> messages = List.of(Msg.builder()
                        .role(MsgRole.USER)
                        .textContent(promptBuilder.buildPrompt(request))
                        .build());
                Msg result = agent.call(messages, KnowledgePageGenerationResponse.class, RuntimeContext.builder().build())
                        .block();
                KnowledgePageGenerationResponse response = responseParser.parse(result, KnowledgePageGenerationResponse.class);
                if (response == null) {
                    String markdown = responseParser.text(result);
                    if (!StringUtils.hasText(markdown)) {
                        throw new BusinessException(KitErrorCode.E03008);
                    }
                    response = new KnowledgePageGenerationResponse(request.fileName(), markdown);
                }
                if (!StringUtils.hasText(response.markdownContent())) {
                    throw new BusinessException(KitErrorCode.E03008);
                }
                String title = StringUtils.hasText(response.title()) ? response.title() : request.fileName();
                return new GeneratedKnowledgePage(title, response.markdownContent());
            }
        } catch (BusinessException ex) {
            if (ex.getCode() == KitErrorCode.E03008) {
                throw ex;
            }
            throw new BusinessException(KitErrorCode.E03008, ex);
        } catch (Exception ex) {
            throw new BusinessException(KitErrorCode.E03008, ex);
        }
    }

    record KnowledgePageGenerationResponse(String title, String markdownContent) {
    }
}
