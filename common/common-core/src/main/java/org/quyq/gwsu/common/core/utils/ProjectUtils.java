package org.quyq.gwsu.common.core.utils;


import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.config.properties.ProjectProperties;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * @author Quyq
 * @date 2026/3/20
 * @description 项目信息获取工具
 */
@RequiredArgsConstructor
public class ProjectUtils {

    private final ProjectProperties projectProperties;

    private final Environment environment;


    /**
     * 获取项目标识
     *
     * @return
     */
    public String getProjectIdent() {
        return projectProperties.ident();
    }


    public String getApplicationName() {
        return environment.getProperty(CoreConstants.Yaml.APPLICATION_NAME);
    }


    /**
     * 获取所属服务的统一前缀
     * {项目标识}.{服务名}
     *
     * @return
     */
    public String getServerPrefix() {
        String systemName = getApplicationName();
        if (StringUtils.hasText(systemName)) {
            return "%s:%s".formatted(getProjectIdent(), systemName);
        }

        return getProjectIdent();
    }

}
