package org.quyq.gwsu.security.dict.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.annotation.LoginAllowAccess;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.common.security.api.IConfigInfoClientApi;
import org.quyq.gwsu.common.security.api.vo.ConfigVO;
import org.quyq.gwsu.security.api.config.dto.ConfigQueryDTO;
import org.quyq.gwsu.security.api.config.dto.ConfigSaveDTO;
import org.quyq.gwsu.security.dict.domain.SecurityConfig;
import org.quyq.gwsu.security.dict.service.ISecurityConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 配置管理控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("config")
@Tag(name = "配置管理", description = "配置管理接口")
@TableModelPermission({SecurityConfig.class})
@RequiredArgsConstructor
public class SecurityConfigController implements IConfigInfoClientApi {

    private final ISecurityConfigService configService;

    @Operation(summary = "根据ID查询配置")
    @GetMapping("/{id}")
    public R<ConfigVO> getById(@PathVariable String id) {
        return R.ok(configService.getById(id));
    }

    @Operation(summary = "根据键查询配置")
    @PostMapping("key/get")
    @LoginAllowAccess
    @Override
    public R<Map<String, ConfigVO>> getByKeys(@RequestBody List<String> keys) {
        return R.ok(configService.getByKeys(keys));
    }


    @Operation(summary = "分页查询配置")
    @PostMapping("/page")
    public R<IPage<ConfigVO>> page(@RequestBody ConfigQueryDTO query) {
        return R.ok(configService.pageByCondition(query));
    }

    @Operation(summary = "新增或更新配置")
    @PostMapping
    public R<Boolean> saveOrUpdate(@RequestBody ConfigSaveDTO dto) {
        return R.ok(configService.saveOrUpdateConfig(dto));
    }

    @Operation(summary = "批量删除配置")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(configService.removeByIds(ids));
    }
}
