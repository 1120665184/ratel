package org.quyq.gwsu.security.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Catalog完整定义（供前端 defineCatalog 使用）")
public class CatalogDefinitionVO {

    @Schema(description = "Catalog标识")
    private String catalogKey;

    @Schema(description = "Catalog名称")
    private String catalogName;

    @Schema(description = "组件定义列表")
    private List<ComponentDefinition> components;

    @Data
    @Schema(description = "组件定义")
    public static class ComponentDefinition {

        @Schema(description = "组件名")
        private String componentName;

        @Schema(description = "组件描述")
        private String description;

        @Schema(description = "属性JSON Schema（字符串）")
        private String propsSchema;

        @Schema(description = "默认属性（字符串）")
        private String defaultProps;

        @Schema(description = "分类")
        private String category;
    }
}
