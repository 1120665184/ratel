package org.quyq.gwsu.security.apiresource.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.apiresource.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.apiresource.vo.TableModelVO;
import org.quyq.gwsu.security.apiresource.service.ISecurityApiTableModelService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 接口-表模型绑定控制器
 */
@RestController
@RequestMapping("apiTableModel")
@Tag(name = "接口-表模型绑定管理", description = "接口-表模型绑定管理接口")
@RequiredArgsConstructor
public class SecurityApiTableModelController {

    private final ISecurityApiTableModelService apiTableModelService;

    @Operation(summary = "分页查询")
    @PostMapping("page")
    public R<IPage<TableModelVO>> page(@RequestBody TableModelQueryDTO query) {
        return R.ok(apiTableModelService.pageByCondition(query));
    }

    @Operation(summary = "根据接口资源ID查询表模型列表")
    @GetMapping("list/by-api/{apiId}")
    public R<List<TableModelVO>> listByApiId(@PathVariable String apiId) {
        return R.ok(apiTableModelService.listByApiId(apiId));
    }

    @Operation(summary = "根据模块前缀查询表模型列表")
    @GetMapping("list/by-module/{modulePrefix}")
    public R<List<TableModelVO>> listByModulePrefix(@PathVariable String modulePrefix) {
        return R.ok(apiTableModelService.listByModulePrefix(modulePrefix));
    }
}
