package org.quyq.gwsu.common.core.utils;


import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.core.env.Environment;

/**
 * @author Quyq
 * @date 2026/3/11
 * @description 部署工具类
 */
public class DeployUtils {

    private DeployUtils() {
    }

    /**
     * 是否是单应用部署
     *
     * @return
     */
    public static boolean isSingle() {
        return Boolean.TRUE.equals(
                SpringUtils.getBean(Environment.class).getProperty(CoreConstants.Yaml.DEPLOY_SINGLE, Boolean.class)
        );
    }


}
