package org.quyq.gwsu.common.deploy.controller;


import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.quyq.gwsu.common.deploy.domain.ApplicationModules;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/3/11
 * @description 单应用服务模式模块信息接口类
 */
@ResponseBody
@RequiredArgsConstructor
@RestController
public class SingleModuleController {

    private final List<BusinessModuleInfoProvider> providers;

    @PostMapping(CoreConstants.EndPoint.ENDPOINT_MODULE_INFOS)
    public R<List<ApplicationModules>> getAllModules() {

        Map<BusinessModuleInfo, String> moduleInfos = new HashMap<>();

        providers.forEach(provider -> moduleInfos.put(provider.module(), provider.applicationName()));

        return R.ok(ApplicationModules.transformationModules(moduleInfos));

    }

}
