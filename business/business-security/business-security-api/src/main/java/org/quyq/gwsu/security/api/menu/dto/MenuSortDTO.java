package org.quyq.gwsu.security.api.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 菜单排序项
 *
 * @author Quyq
 */
@Data
@Schema(description = "菜单排序项")
public class MenuSortDTO {

    @Schema(description = "菜单ID")
    private String id;

    @Schema(description = "父菜单ID")
    private String parentId;

    @Schema(description = "排序号")
    private Integer sort;
}
