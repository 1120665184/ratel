package org.quyq.gwsu.security.catalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponent;
import org.quyq.gwsu.security.catalog.service.ISecurityCatalogComponentService;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogComponentVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catalog组件管理控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("catalog/component")
@Tag(name = "Catalog组件管理", description = "Catalog组件管理接口")
@RequiredArgsConstructor
@TableModelPermission({SecurityCatalogComponent.class})
public class SecurityCatalogComponentController {

    private final ISecurityCatalogComponentService componentService;

    @Operation(summary = "查询所有组件列表")
    @GetMapping("/list")
    public R<List<SecurityCatalogComponentVO>> listAll() {
        return R.ok(componentService.listAll());
    }

    @Operation(summary = "根据ID查询组件")
    @GetMapping("/{id}")
    public R<SecurityCatalogComponentVO> getById(@PathVariable String id) {
        return R.ok(componentService.getComponentById(id));
    }

    @Operation(summary = "新增或更新组件")
    @PostMapping
    public R<String> saveOrUpdate(@RequestBody SecurityCatalogComponentVO vo) {
        return R.ok(componentService.saveOrUpdateComponent(vo));
    }

    @Operation(summary = "批量删除组件")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(componentService.removeComponents(ids));
    }
}
