package org.quyq.gwsu.kit.knowledge.engine.image;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Base64Source;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.Model;
import org.quyq.gwsu.common.ai.model.ModelProvider;
import org.quyq.gwsu.kit.knowledge.engine.model.AgentScopeResponseParser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.List;

/**
 * 知识库图片 OCR / 描述服务。
 */
@Service
public class KnowledgeImageOcrService {

    private static final String SYSTEM_PROMPT = """
            你是知识库导入阶段的图片识别助手。
            请基于图片本身提取可检索的客观描述，优先识别图片中的文字、标题、图表名称、关键标签、步骤说明。
            输出必须是单段纯文本，不要 Markdown，不要编号，不要前缀，不要解释。
            如果图片中有文字，请尽量按原文提取；如果主要是图表或示意图，请给出简洁事实描述。
            """;

    private final AgentScopeResponseParser responseParser;

    public KnowledgeImageOcrService(AgentScopeResponseParser responseParser) {
        this.responseParser = responseParser;
    }

    public KnowledgeImageOcrResult recognize(byte[] imageBytes, String fileName, String contentType) {
        if (imageBytes == null || imageBytes.length == 0) {
            return KnowledgeImageOcrResult.skipped("");
        }
        if (!ModelProvider.supportMultimodal()) {
            return KnowledgeImageOcrResult.skipped("当前模型未开启多模态能力，已跳过图片 OCR 解析。");
        }
        try {
            Model model = ModelProvider.generateModel();
            try (ReActAgent agent = ReActAgent.builder()
                    .name("knowledgeImageOcr")
                    .sysPrompt(SYSTEM_PROMPT)
                    .model(model)
                    .build()) {
                UserMessage message = new UserMessage(List.of(
                        TextBlock.builder()
                                .text(buildPrompt(fileName))
                                .build(),
                        ImageBlock.builder()
                                .source(Base64Source.builder()
                                        .mediaType(resolveContentType(contentType))
                                        .data(Base64.getEncoder().encodeToString(imageBytes))
                                        .build())
                                .build()));
                Msg result = agent.call(List.of(message), RuntimeContext.builder().build()).block();
                String text = KnowledgeImageMarkerSupport.sanitizeAltText(responseParser.text(result));
                if (!StringUtils.hasText(text)) {
                    return KnowledgeImageOcrResult.skipped("图片 OCR 未返回可用描述，已保留图片但未写入 alt 文本。");
                }
                return KnowledgeImageOcrResult.success(text);
            }
        } catch (Exception ex) {
            return KnowledgeImageOcrResult.skipped("图片 OCR 解析失败，已保留图片原位。");
        }
    }

    private String buildPrompt(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "请识别这张图片，并返回适合放入 Markdown 图片 alt 的简洁描述。";
        }
        return "文件名：" + fileName + "\n请识别这张图片，并返回适合放入 Markdown 图片 alt 的简洁描述。";
    }

    private String resolveContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : "image/png";
    }
}
