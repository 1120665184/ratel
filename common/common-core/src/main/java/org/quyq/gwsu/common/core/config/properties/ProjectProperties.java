package org.quyq.gwsu.common.core.config.properties;


import io.swagger.v3.oas.annotations.media.Schema;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Quyq
 * @date 2026/3/20
 * @description
 */
@ConfigurationProperties(CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".project")
public record ProjectProperties(
        @Schema(title = "项目标识" , description = "项目缓存key依赖该配置做统一前缀")
        String ident,
        @Schema(title = "所属公司")
        String company,
        @Schema(title = "联系方式")
        String telephone,
        @Schema(title = "官网地址")
        String website
) {

    public ProjectProperties {
        if (ident == null) {
            ident = "gwsu";
        }
    }

}
