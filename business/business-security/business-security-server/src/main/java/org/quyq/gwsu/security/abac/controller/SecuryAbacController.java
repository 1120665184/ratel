package org.quyq.gwsu.security.abac.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.security.abac.domain.SecurityAbac;
import org.quyq.gwsu.security.abac.domain.SecurityAbacField;
import org.quyq.gwsu.security.abac.domain.SecurityAbacPermission;
import org.quyq.gwsu.security.abac.service.ISecurityAbacService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("abac/expression")
@Tag(name = "ABAC表达式模块", description = "ABAC表达式管理")
@TableModelPermission({SecurityAbac.class, SecurityAbacPermission.class, SecurityAbacField.class})
@RequiredArgsConstructor
public class SecuryAbacController {

    private final ISecurityAbacService securyAbacService;

    @Operation(summary = "获取所有表达式")
    @GetMapping
    public R<List<SecurityAbac>> list() {
        return R.ok(securyAbacService.list());
    }

    @Operation(summary = "根据ID获取表达式")
    @GetMapping("{id}")
    public R<SecurityAbac> getById(@PathVariable String id) {
        return R.ok(securyAbacService.getById(id));
    }

    @Operation(summary = "创建表达式")
    @PostMapping
    public R<Boolean> save(@RequestBody SecurityAbac securityAbac) {
        return R.ok(securyAbacService.save(securityAbac));
    }

    @Operation(summary = "更新表达式")
    @PutMapping
    public R<Boolean> updateById(@RequestBody SecurityAbac securityAbac) {
        return R.ok(securyAbacService.updateById(securityAbac));
    }

    @Operation(summary = "删除表达式")
    @DeleteMapping("/{id}")
    public R<Boolean> removeById(@PathVariable String id) {
        return R.ok(securyAbacService.removeById(id));
    }

}
