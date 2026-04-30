package org.quyq.gwsu.common.deploy.domain;


import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/3/12
 * @description
 */
public record ApplicationModules(
        //接口前缀
        String prefix,
        //对应服务名
        String applicationName,
        //模块描述
        String note
) {

    public static List<ApplicationModules> transformationModules(Map<BusinessModuleInfo, String> infos) {
        List<ApplicationModules> finV = new ArrayList<>();
        infos.forEach((meta, applicationName) -> {
            finV.add(new ApplicationModules(meta.prefix(), applicationName, meta.note()));
        });
        return finV;
    }

}
