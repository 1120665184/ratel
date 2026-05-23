package org.quyq.gwsu.security.catalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalog;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponentRef;
import org.quyq.gwsu.security.catalog.service.ISecurityCatalogService;
import org.quyq.gwsu.security.catalog.service.ISecurityCatalogComponentService;
import org.quyq.gwsu.security.catalog.vo.CatalogDefinitionVO;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogComponentVO;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catalog管理控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("catalog")
@Tag(name = "Catalog管理", description = "Catalog管理接口")
@RequiredArgsConstructor
@TableModelPermission({SecurityCatalog.class, SecurityCatalogComponentRef.class})
public class SecurityCatalogController {

    private final ISecurityCatalogService catalogService;

    private final ISecurityCatalogComponentService componentService;

    @Operation(summary = "查询所有Catalog列表")
    @GetMapping("/list")
    public R<List<SecurityCatalogVO>> listAll() {
        return R.ok(catalogService.listAll());
    }

    @Operation(summary = "根据ID查询Catalog")
    @GetMapping("/{id}")
    public R<SecurityCatalogVO> getById(@PathVariable String id) {
        return R.ok(catalogService.getCatalogById(id));
    }

    @Operation(summary = "新增或更新Catalog")
    @PostMapping
    public R<String> saveOrUpdate(@RequestBody SecurityCatalogVO vo) {
        return R.ok(catalogService.saveOrUpdateCatalog(vo));
    }

    @Operation(summary = "批量删除Catalog")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(catalogService.removeCatalogs(ids));
    }

    @Operation(summary = "激活Catalog")
    @PutMapping("/activate/{id}")
    public R<Boolean> activate(@PathVariable String id) {
        return R.ok(catalogService.activateCatalog(id));
    }

    @Operation(summary = "获取当前激活的Catalog完整定义")
    @GetMapping("/active-definition")
    public R<CatalogDefinitionVO> getActiveDefinition() {
        return R.ok(catalogService.getActiveCatalogDefinition());
    }

    @Operation(summary = "根据catalogKey获取Catalog完整定义")
    @GetMapping("/definition/{catalogKey}")
    public R<CatalogDefinitionVO> getDefinitionByKey(@PathVariable String catalogKey) {
        return R.ok(catalogService.getCatalogDefinitionByKey(catalogKey));
    }

    @Operation(summary = "给Catalog绑定组件列表（全量替换）")
    @PutMapping("/{catalogId}/components")
    public R<Boolean> bindComponents(@PathVariable String catalogId, @RequestBody List<String> componentIds) {
        return R.ok(catalogService.bindComponents(catalogId, componentIds));
    }

    @Operation(summary = "解绑Catalog的组件")
    @DeleteMapping("/{catalogId}/components/{componentId}")
    public R<Boolean> unbindComponent(@PathVariable String catalogId, @PathVariable String componentId) {
        return R.ok(catalogService.unbindComponent(catalogId, componentId));
    }

    @Operation(summary = "获取Catalog已绑定的组件ID列表")
    @GetMapping("/{catalogId}/component-ids")
    public R<List<String>> getBoundComponentIds(@PathVariable String catalogId) {
        return R.ok(catalogService.getBoundComponentIds(catalogId));
    }

    @Operation(summary = "获取Catalog已绑定的组件详情列表")
    @GetMapping("/{catalogId}/components")
    public R<List<SecurityCatalogComponentVO>> getBoundComponents(@PathVariable String catalogId) {
        return R.ok(catalogService.getBoundComponents(catalogId));
    }
}
