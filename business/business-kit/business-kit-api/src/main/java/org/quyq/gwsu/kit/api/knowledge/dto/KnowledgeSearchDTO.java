package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

import java.util.List;

/**
 * 知识库检索条件。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "知识库检索条件")
public class KnowledgeSearchDTO extends BaseDTO {

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "当前用户角色编码")
    private List<String> roleCodes;

    @Schema(description = "返回数量")
    private Integer size;
}
