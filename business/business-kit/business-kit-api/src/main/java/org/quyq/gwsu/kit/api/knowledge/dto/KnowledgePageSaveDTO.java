package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgePageBlockVO;

import java.util.List;

/**
 * 知识 Page 保存参数。
 */
@Data
@Schema(description = "知识Page保存参数")
public class KnowledgePageSaveDTO {

    @Schema(description = "Page ID")
    private String id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "Block列表")
    private List<KnowledgePageBlockVO> blocks;
}
