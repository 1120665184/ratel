package org.quyq.gwsu.system.common;


import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.authentication.dataresource.DataResourceAttributeProvider;
import org.quyq.gwsu.common.authentication.dataresource.domain.ResourceRuleKeyProperties;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.system.api.common.dto.UserDTO;
import org.quyq.gwsu.system.api.manager.dto.SysUserQueryDTO;
import org.quyq.gwsu.system.api.manager.vo.UserVO;
import org.quyq.gwsu.system.manager.service.ISysUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/5/4
 * @description
 */
@RestController
@RequestMapping("basic")
@Tag(name = "基础接口模块")
@RequiredArgsConstructor
public class BasicController {

    private final List<DataResourceAttributeProvider> attributeProviders;

    private final ISysUserService userService;

    @Operation(summary = "获取数据资源属性列表，用于数据权限配置的选项")
    @GetMapping("dataResourceAttribute")
    public R<List<ResourceRuleKeyProperties>> getDataResourceAttribute() {
        return R.ok(attributeProviders
                .stream().map(DataResourceAttributeProvider::keyInfo).toList());
    }

    @Operation(summary = "分页获取用户信息")
    @GetMapping("page/userInfo")
    public R<IPage<UserVO>> userPage(@RequestBody UserDTO dto) {
        SysUserQueryDTO form = new SysUserQueryDTO();
        form.setStatus(1);
        form.setKeyword(dto.getSearch());
        form.setPageNum(dto.getPageNum());
        form.setPageSize(dto.getPageSize());
        form.setAsc(dto.getAsc());
        form.setOrderByColumn(dto.getOrderByColumn());
        return R.ok(userService.pageByCondition(form));
    }


}
