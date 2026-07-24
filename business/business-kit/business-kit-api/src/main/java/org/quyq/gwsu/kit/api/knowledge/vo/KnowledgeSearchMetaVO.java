package org.quyq.gwsu.kit.api.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 知识检索元信息。
 */
@Data
@Accessors(chain = true)
@Schema(description = "知识检索元信息")
public class KnowledgeSearchMetaVO {

    @Schema(description = "知识库 Wiki 页面统一语言")
    private String wikiPageLanguage;
}
