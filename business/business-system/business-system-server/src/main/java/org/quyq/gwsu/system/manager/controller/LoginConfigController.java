package org.quyq.gwsu.system.manager.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.config.properties.universal.BaseProjectInfoProperties;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.quyq.gwsu.system.api.manager.vo.LoginInfoVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Quyq
 * @date 2026/7/2
 * @description 获取登录页的基础配置信息（该接口无需认证）
 */
@RestController
@RequestMapping("auth")
@Tag(name = "登录基础信息模块")
public class LoginConfigController {


    @GetMapping("configInfo")
    @Operation(description = "登录页的基础信息配置获取")
    public R<LoginInfoVO> configInfo() {

        BaseProjectInfoProperties projectInfo = ConfigInfoUtils.getByObject(BaseProjectInfoProperties.CONFIG_KEY, BaseProjectInfoProperties.class);

        LoginInfoVO info = new LoginInfoVO(projectInfo.projectName());

        return R.ok(info);

    }

}
