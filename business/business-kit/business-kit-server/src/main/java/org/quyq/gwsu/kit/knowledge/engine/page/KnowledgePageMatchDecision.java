package org.quyq.gwsu.kit.knowledge.engine.page;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Page 归属决策。
 *
 * @param action 归属动作
 * @param pageId 命中的 Page ID
 * @param confidence 置信度，0~1
 * @param reason 决策原因
 */
public record KnowledgePageMatchDecision(
        @JsonPropertyDescription("归属动作，只能是 MATCH_EXISTING_PAGE 或 CREATE_NEW_PAGE")
        @Schema(description = "归属动作，只能是 MATCH_EXISTING_PAGE 或 CREATE_NEW_PAGE",
                allowableValues = {"MATCH_EXISTING_PAGE", "CREATE_NEW_PAGE"})
        KnowledgePageMatchAction action,
        @JsonPropertyDescription("当 action 为 MATCH_EXISTING_PAGE 时填写候选 Page 的 pageId；当 action 为 CREATE_NEW_PAGE 时为空字符串")
        String pageId,
        @JsonPropertyDescription("置信度，范围 0 到 1")
        double confidence,
        @JsonPropertyDescription("简要说明为什么选择这个动作")
        String reason) {

    public boolean matchedExistingPage() {
        return KnowledgePageMatchAction.MATCH_EXISTING_PAGE == action
                && pageId != null
                && !pageId.isBlank()
                && confidence >= 0.65D;
    }
}
