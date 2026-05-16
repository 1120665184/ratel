package org.quyq.gwsu.common.core.utils;


import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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


    /**
     * 分布式模式部署时，获取服务名与服务模块前缀的映射关系
     *
     * @return
     */
    public static Map<String, String> getDistributedServerModuleMapping() {

        if (isSingle() || !ProxyUtil.hasClass("org.springframework.cloud.client.discovery.DiscoveryClient")) {
            return Map.of();
        }

        DiscoveryClient discoveryClient = SpringUtils.getBean(DiscoveryClient.class);
        List<String> services = discoveryClient
                .getServices();
        if (CollectionUtils.isEmpty(services)) {
            return Map.of();
        }

        Map<String, String> result = new HashMap<>();
        for (String service : services) {
            Optional<String> prefix = Optional.ofNullable(discoveryClient.getInstances(service)
                            .getFirst()
                            .getMetadata())
                    .map(v -> v.get("prefix"));

            if (prefix.isEmpty()) {
                continue;
            }
            result.put(prefix.get(), service);
        }

        return result;
    }


}
