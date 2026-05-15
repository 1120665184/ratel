package org.quyq.gwsu.security.apiresource.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.apiresource.dto.TableModelConfigSaveDTO;
import org.quyq.gwsu.security.api.apiresource.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.apiresource.vo.TableModelConfigVO;
import org.quyq.gwsu.security.apiresource.service.ISecurityApiTableModelConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 表模型手动配置控制器
 */
@RestController
@RequestMapping("apiTableModelConfig")
@Tag(name = "表模型配置管理", description = "表模型手动配置管理接口")
@RequiredArgsConstructor
public class SecurityApiTableModelConfigController {

    private final ISecurityApiTableModelConfigService configService;

    @Operation(summary = "分页查询")
    @PostMapping("page")
    public R<IPage<TableModelConfigVO>> page(@RequestBody TableModelQueryDTO query) {
        return R.ok(configService.pageByCondition(query));
    }

    @Operation(summary = "保存或更新配置")
    @PostMapping
    public R<Boolean> saveOrUpdate(@RequestBody TableModelConfigSaveDTO dto) {
        return R.ok(configService.saveOrUpdateConfig(dto));
    }

    @Operation(summary = "根据表模型绑定ID查询配置")
    @GetMapping("by-table-model/{tableModelId}")
    public R<TableModelConfigVO> getByTableModelId(@PathVariable String tableModelId) {
        return R.ok(configService.getByTableModelId(tableModelId));
    }

    @Operation(summary = "查询独立表模型列表")
    @GetMapping("independent")
    public R<List<TableModelConfigVO>> listIndependent() {
        return R.ok(configService.listIndependent());
    }
}
