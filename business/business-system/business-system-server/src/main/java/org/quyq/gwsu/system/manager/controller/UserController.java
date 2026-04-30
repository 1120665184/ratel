package org.quyq.gwsu.system.manager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.annotation.LoginAllowAccess;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.system.api.manager.dto.ResetPasswordDTO;
import org.quyq.gwsu.system.api.manager.dto.SysAccountBindDTO;
import org.quyq.gwsu.system.api.manager.dto.SysUserQueryDTO;
import org.quyq.gwsu.system.api.manager.vo.SysUserDetailVO;
import org.quyq.gwsu.system.api.manager.vo.UserVO;
import org.quyq.gwsu.system.errcode.SystemErrorCode;
import org.quyq.gwsu.system.manager.service.ISysUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("manager")
@Tag(name = "用户管理")
@RequiredArgsConstructor
public class UserController {

    private final ISysUserService userService;
    private final SecurityUtils securityUtils;

    @LoginAllowAccess
    @GetMapping("current")
    @Operation(summary = "获取当前登录用户信息")
    public R<SysUserDetailVO> currentUserInfo() {
        return securityUtils.userInfo()
                .map(v -> R.ok((SysUserDetailVO) v))
                .orElseGet(R::ok);
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询用户")
    public R<IPage<UserVO>> page(@RequestBody SysUserQueryDTO query) {
        return R.ok(userService.pageByCondition(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情")
    public R<SysUserDetailVO> getDetailById(@PathVariable String id) {
        return R.ok(userService.getDetailById(id));
    }

    @PostMapping
    @Operation(summary = "新增或编辑用户")
    public R<String> saveOrUpdate(@RequestBody UserVO vo) {
        if (vo.getUserId() == null) {
            AssertUtils.hasText(vo.getUserName(), SystemErrorCode.E02006);
            AssertUtils.hasText(vo.getPassword(), SystemErrorCode.E02007);
        }
        return R.ok(userService.saveOrUpdateUser(vo));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启禁用用户")
    public R<Void> updateStatus(@PathVariable String id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return R.ok();
    }

    @PostMapping("/{id}/account")
    @Operation(summary = "绑定账号")
    public R<Void> bindAccount(@PathVariable String id, @RequestBody SysAccountBindDTO dto) {
        userService.bindAccount(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}/account/{accountId}")
    @Operation(summary = "解绑账号")
    public R<Void> unbindAccount(@PathVariable String id, @PathVariable String accountId) {
        userService.unbindAccount(id, accountId);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "批量删除用户")
    public R<Void> removeUsers(@RequestBody List<String> ids) {
        userService.removeUsers(ids);
        return R.ok();
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "重置用户密码")
    public R<Void> resetPassword(@PathVariable String id, @RequestBody ResetPasswordDTO dto) {
        AssertUtils.hasText(dto.getNewPassword(), SystemErrorCode.E02009);
        userService.resetPassword(id, dto.getNewPassword());
        return R.ok();
    }
}
