package org.quyq.gwsu.system.dept.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.system.api.dept.dto.DeptSaveDTO;
import org.quyq.gwsu.system.api.dept.enums.DeptTypeEnum;
import org.quyq.gwsu.system.api.dept.vo.DeptTreeVO;
import org.quyq.gwsu.system.api.dept.vo.DeptTypeVO;
import org.quyq.gwsu.system.api.dept.vo.DeptVO;
import org.quyq.gwsu.system.api.dept.vo.UserDeptDetailVO;
import org.quyq.gwsu.system.dept.service.ISysDeptService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 部门管理控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("dept")
@Tag(name = "部门管理")
@RequiredArgsConstructor
public class DeptController {

    private final ISysDeptService deptService;

    @GetMapping("/types")
    @Operation(summary = "获取部门类型列表")
    public R<List<DeptTypeVO>> getTypes() {
        List<DeptTypeVO> types = Arrays.stream(DeptTypeEnum.values())
                .map(e -> new DeptTypeVO(e.getCode(), e.getName()))
                .toList();
        return R.ok(types);
    }

    @PostMapping
    @Operation(summary = "保存部门", description = "新增或更新部门，有ID为更新，无ID为新增")
    public R<String> save(@RequestBody DeptSaveDTO dto) {
        return R.ok(deptService.saveDept(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除部门")
    public R<Void> remove(@PathVariable String id) {
        deptService.removeDept(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取部门详情")
    public R<DeptVO> getDetail(@PathVariable String id) {
        return R.ok(deptService.getDeptDetail(id));
    }

    @GetMapping("/tree")
    @Operation(summary = "获取部门树")
    public R<List<DeptTreeVO>> getTree() {
        return R.ok(deptService.getDeptTree());
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "获取子部门列表")
    public R<List<DeptVO>> getChildren(@PathVariable String id) {
        return R.ok(deptService.listChildren(id));
    }

    @PostMapping("/{id}/parent/{parentId}")
    @Operation(summary = "添加父部门")
    public R<Void> addParent(@PathVariable String id, @PathVariable String parentId) {
        deptService.addParent(id, parentId);
        return R.ok();
    }

    @DeleteMapping("/{id}/parent/{parentId}")
    @Operation(summary = "移除父部门")
    public R<Void> removeParent(@PathVariable String id, @PathVariable String parentId) {
        deptService.removeParent(id, parentId);
        return R.ok();
    }

    @GetMapping("/{id}/users")
    @Operation(summary = "获取部门下用户")
    public R<List<UserDeptDetailVO>> getUsers(@PathVariable String id) {
        return R.ok(deptService.listUsersByDept(id));
    }

    @GetMapping("/user-count")
    @Operation(summary = "获取各部门用户数量")
    public R<Map<String, Long>> getDeptUserCount() {
        return R.ok(deptService.countUsersByDept());
    }
}