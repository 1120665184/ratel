package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeChunkDirection;

import java.util.List;

/**
 * 知识 Chunk 邻近查询条件。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "知识Chunk邻近查询条件")
public class KnowledgeChunkAdjacentDTO extends BaseDTO {

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "当前用户角色编码")
    private List<String> roleCodes;

    @Schema(description = "当前Chunk ID")
    private String chunkId;

    @Schema(description = "邻近查询方向")
    private KnowledgeChunkDirection direction;
}
