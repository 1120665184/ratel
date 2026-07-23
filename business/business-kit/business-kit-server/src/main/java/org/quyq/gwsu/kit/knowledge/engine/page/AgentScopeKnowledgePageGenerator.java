package org.quyq.gwsu.kit.knowledge.engine.page;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.Model;
import lombok.extern.slf4j.Slf4j;
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
import java.util.regex.Pattern;

/**
 * 基于 AgentScope 的知识 Page 生成器。
 */
@Component
@Slf4j
@RegisterReflectionForBinding(AgentScopeKnowledgePageGenerator.KnowledgePageGenerationResponse.class)
public class AgentScopeKnowledgePageGenerator implements KnowledgePageGenerator {

    private static final Pattern MEANINGLESS_CONTENT_PATTERN = Pattern.compile("^[\\s`#>*\\-+|._~…。，、；：？！()\\[\\]{}]+$");

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
        if (!StringUtils.hasText(request.sourceContext())) {
            throw new BusinessException(KitErrorCode.E03006);
        }
        log.debug("开始生成知识 Wiki 页面: fileName={}, sourceLanguage={}, outputLanguage={}, digestLength={}, sourceContextLength={}",
                request.fileName(),
                request.sourceLanguage(),
                request.outputLanguage(),
                safeLength(request.analysisDigest()),
                safeLength(request.sourceContext()));
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
                log.debug("知识 Wiki 页面模型原始返回: fileName={}, rawTextPreview={}",
                        request.fileName(),
                        abbreviate(responseParser.text(result), 300));
                KnowledgePageGenerationResponse response = responseParser.parse(result, KnowledgePageGenerationResponse.class);
                if (response == null) {
                    String markdown = responseParser.text(result);
                    if (!StringUtils.hasText(markdown)) {
                        log.warn("知识 Wiki 页面生成失败，模型无可解析文本: fileName={}", request.fileName());
                        throw new BusinessException(KitErrorCode.E03008);
                    }
                    log.warn("知识 Wiki 页面结构化解析失败，回退为纯文本 Markdown: fileName={}, markdownPreview={}",
                            request.fileName(),
                            abbreviate(markdown, 300));
                    response = new KnowledgePageGenerationResponse(request.fileName(), markdown);
                }
                if (!StringUtils.hasText(response.markdownContent())) {
                    log.warn("知识 Wiki 页面生成失败，markdownContent 为空: fileName={}, titlePreview={}",
                            request.fileName(),
                            abbreviate(response.title(), 120));
                    throw new BusinessException(KitErrorCode.E03008);
                }
                String title = StringUtils.hasText(response.title()) ? response.title() : request.fileName();
                if (isMeaninglessContent(title)) {
                    log.warn("知识 Wiki 页面标题疑似占位内容，回退文件名: fileName={}, rawTitle={}",
                            request.fileName(),
                            abbreviate(title, 120));
                    title = request.fileName();
                }
                if (isMeaninglessContent(title) || isMeaninglessContent(response.markdownContent())) {
                    log.warn("知识 Wiki 页面生成失败，检测到占位内容: fileName={}, titlePreview={}, markdownPreview={}",
                            request.fileName(),
                            abbreviate(title, 120),
                            abbreviate(response.markdownContent(), 300));
                    throw new BusinessException(KitErrorCode.E03008);
                }
                log.info("知识 Wiki 页面生成完成: fileName={}, title={}, markdownLength={}",
                        request.fileName(),
                        abbreviate(title, 120),
                        response.markdownContent().trim().length());
                return new GeneratedKnowledgePage(title, response.markdownContent().trim());
            }
        } catch (BusinessException ex) {
            if (ex.getCode() == KitErrorCode.E03008) {
                log.warn("知识 Wiki 页面生成异常: fileName={}, code={}, message={}",
                        request.fileName(),
                        ex.getCode(),
                        ex.getMessage());
                throw ex;
            }
            throw new BusinessException(KitErrorCode.E03008, ex);
        } catch (Exception ex) {
            log.error("知识 Wiki 页面生成发生未预期异常: fileName={}", request.fileName(), ex);
            throw new BusinessException(KitErrorCode.E03008, ex);
        }
    }

    record KnowledgePageGenerationResponse(String title, String markdownContent) {
    }

    private boolean isMeaninglessContent(String text) {
        if (!StringUtils.hasText(text)) {
            return true;
        }
        String normalized = text.trim();
        if (normalized.length() <= 3 && normalized.chars().allMatch(ch -> ch == '.' || ch == '。' || ch == '…')) {
            return true;
        }
        if (MEANINGLESS_CONTENT_PATTERN.matcher(normalized).matches()) {
            return true;
        }
        String collapsed = normalized
                .replaceAll("```[\\s\\S]*?```", " ")
                .replaceAll("[`#>*\\-+|_~\\s]", "")
                .trim();
        if (!StringUtils.hasText(collapsed)) {
            return true;
        }
        return collapsed.chars().allMatch(ch -> ch == '.' || ch == '。' || ch == '…');
    }

    private int safeLength(String text) {
        return text == null ? 0 : text.length();
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
