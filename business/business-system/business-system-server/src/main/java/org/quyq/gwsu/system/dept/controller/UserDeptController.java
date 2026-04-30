package org.quyq.gwsu.system.dept.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.system.api.dept.dto.RemoveUserDeptDTO;
import org.quyq.gwsu.system.api.dept.dto.SetPrimaryDeptDTO;
import org.quyq.gwsu.system.api.dept.dto.UserDeptSaveDTO;
import org.quyq.gwsu.system.api.dept.vo.UserDeptDetailVO;
import org.quyq.gwsu.system.dept.service.ISysUserDeptService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户部门关联控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("user-dept")
@Tag(name = "用户部门关联")
@RequiredArgsConstructor
public class UserDeptController {

    private final ISysUserDeptService userDeptService;

    @PostMapping
    @Operation(summary = "设置用户部门")
    public R<Void> save(@RequestBody UserDeptSaveDTO dto) {
        userDeptService.saveUserDept(dto);
        return R.ok();
    }

    @PutMapping("/primary")
    @Operation(summary = "设置主部门")
    public R<Void> setPrimary(@RequestBody SetPrimaryDeptDTO dto) {
        userDeptService.setPrimaryDept(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "移除用户部门")
    public R<Void> remove(@RequestBody RemoveUserDeptDTO dto) {
        userDeptService.removeUserDept(dto);
        return R.ok();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户所属部门")
    public R<List<UserDeptDetailVO>> getDeptsByUser(@PathVariable String userId) {
        return R.ok(userDeptService.listDeptsByUser(userId));
    }
}