package org.quyq.gwsu.system.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.authentication.constants.AuthenticationConstants;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.system.api.dept.dto.UserDeptSaveDTO;
import org.quyq.gwsu.system.api.dept.vo.UserDeptDetailVO;
import org.quyq.gwsu.system.api.manager.dto.SysAccountBindDTO;
import org.quyq.gwsu.system.api.manager.dto.SysUserQueryDTO;
import org.quyq.gwsu.system.api.manager.vo.AccountVO;
import org.quyq.gwsu.system.api.manager.vo.SysUserDeptVO;
import org.quyq.gwsu.system.api.manager.vo.SysUserDetailVO;
import org.quyq.gwsu.system.api.manager.vo.UserVO;
import org.quyq.gwsu.system.dept.domain.SysUserDept;
import org.quyq.gwsu.system.dept.service.ISysUserDeptService;
import org.quyq.gwsu.system.errcode.SystemErrorCode;
import org.quyq.gwsu.system.manager.domain.SysAccount;
import org.quyq.gwsu.system.manager.domain.SysUser;
import org.quyq.gwsu.system.manager.mapper.SysAccountMapper;
import org.quyq.gwsu.system.manager.mapper.SysUserMapper;
import org.quyq.gwsu.system.manager.service.ISysUserService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private final SysAccountMapper accountMapper;
    private final ISysUserDeptService userDeptService;

    @Override
    public SysUser getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
    }

    @Override
    public IPage<UserVO> pageByCondition(SysUserQueryDTO query) {
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<SysUser> sysUserIPage = baseMapper.selectUserPage(page, query);
        Page<UserVO> fin = Page.of(sysUserIPage.getCurrent(), sysUserIPage.getSize(), sysUserIPage.getTotal());
        if (!CollectionUtils.isEmpty(sysUserIPage.getRecords())) {
            fin.setRecords(
                    sysUserIPage.getRecords()
                            .stream().map(SysUser::toVo)
                            .toList()
            );
        }
        return fin;
    }

    @Override
    public SysUserDetailVO getDetailById(String id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException(SystemErrorCode.E02005);
        }
        SysUserDetailVO detail = new SysUserDetailVO();
        detail.setUserId(user.getId());
        detail.setUserName(user.getUsername());
        detail.setNickname(user.getNickname());
        detail.setAvatar(user.getAvatar());
        detail.setEmail(user.getEmail());
        detail.setPhone(user.getPhone());
        detail.setGender(user.getGender());
        detail.setStatus(user.getStatus());
        detail.setLastLoginTime(user.getLastLoginTime());
        detail.copyBaseProperties(user);

        List<AccountVO> accounts = accountMapper.selectList(
                        new LambdaQueryWrapper<SysAccount>()
                                .eq(SysAccount::getUserId, id))
                .stream()
                .map(SysAccount::toVo)
                .toList();
        detail.setAccounts(accounts);

        List<UserDeptDetailVO> deptDetails = userDeptService.listDeptsByUser(id);
        List<SysUserDeptVO> depts = deptDetails.stream().map(d -> {
            SysUserDeptVO vo = new SysUserDeptVO();
            vo.setId(d.getId());
            vo.setDeptId(d.getDeptId());
            vo.setDeptName(d.getDeptName());
            vo.setIsPrimary(d.getIsPrimary());
            return vo;
        }).toList();
        detail.setDepts(depts);

        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrUpdateUser(UserVO vo) {
        if (vo.getUserId() != null) {
            SysUser user = getById(vo.getUserId());
            if (user == null) {
                throw new BusinessException(SystemErrorCode.E02005);
            }

            updateById(SysUser.toDo(vo));

            return user.getId();
        } else {
            if (getByUsername(vo.getUserName()) != null) {
                throw new BusinessException(SystemErrorCode.E02001);
            }
            SysUser user = SysUser.toDo(vo);
            user.setGender(vo.getGender() != null ? vo.getGender() : 0);
            user.setStatus(vo.getStatus() != null ? vo.getStatus() : 1);
            save(user);

            SysAccount account = new SysAccount();
            account.setUserId(user.getId());
            account.setIdentityType(AuthenticationConstants.LoginType.PASSWORD);
            account.setIdentifier(vo.getUserName());
            account.setCredential(BCrypt.hashpw(vo.getPassword(), BCrypt.gensalt()));
            account.setStatus(1);
            account.setVerified(true);
            account.setBindTime(LocalDateTime.now());
            accountMapper.insert(account);

            // 如果有部门信息，保存用户部门关联
            if (vo.getDeptId() != null) {
                UserDeptSaveDTO dto = new UserDeptSaveDTO();
                dto.setUserId(user.getId());
                dto.setDeptIds(List.of(vo.getDeptId()));
                dto.setPrimaryDeptId(vo.getDeptId());
                userDeptService.saveUserDept(dto);
            }

            return user.getId();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String id, Integer status) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException(SystemErrorCode.E02005);
        }
        user.setStatus(status);
        updateById(user);
        accountMapper.update(null, new LambdaUpdateWrapper<SysAccount>()
                .eq(SysAccount::getUserId, id)
                .set(SysAccount::getStatus, status));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindAccount(String userId, SysAccountBindDTO dto) {
        Long count = accountMapper.selectCount(new LambdaQueryWrapper<SysAccount>()
                .eq(SysAccount::getIdentifier, dto.getIdentifier()));
        if (count > 0) {
            if ("phone".equals(dto.getIdentityType())) {
                throw new BusinessException(SystemErrorCode.E02002);
            }
            throw new BusinessException(SystemErrorCode.E02003);
        }

        SysAccount account = new SysAccount();
        account.setUserId(userId);
        account.setIdentityType(dto.getIdentityType());
        account.setIdentifier(dto.getIdentifier());
        if (dto.getCredential() != null) {
            account.setCredential(BCrypt.hashpw(dto.getCredential(), BCrypt.gensalt()));
        }
        account.setStatus(1);
        account.setVerified(false);
        account.setBindTime(LocalDateTime.now());
        accountMapper.insert(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindDingTalkAccount(String userId, SysAccountBindDTO dto) {
        // 1. 如果有 originalUserId，表示切换绑定，需要删除原钉钉用户
        if (dto.getOriginalUserId() != null) {
            String originalUserId = dto.getOriginalUserId();
            // 删除原用户的所有账号记录
            accountMapper.delete(new LambdaQueryWrapper<SysAccount>()
                    .eq(SysAccount::getUserId, originalUserId));
            // 删除原用户-部门关联
            userDeptService.remove(new LambdaQueryWrapper<SysUserDept>()
                    .eq(SysUserDept::getUserId, originalUserId));
            // 删除原用户本身
            removeById(originalUserId);
        }

        // 2. 为当前用户创建钉钉绑定
        SysAccount account = new SysAccount();
        account.setUserId(userId);
        account.setIdentityType("dingtalk");
        account.setIdentifier(dto.getIdentifier());
        account.setStatus(1);
        account.setVerified(true);
        account.setBindTime(LocalDateTime.now());
        accountMapper.insert(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindAccount(String userId, String accountId) {
        Long count = accountMapper.selectCount(new LambdaQueryWrapper<SysAccount>()
                .eq(SysAccount::getUserId, userId));
        if (count <= 1) {
            throw new BusinessException(SystemErrorCode.E02004);
        }
        accountMapper.delete(new LambdaQueryWrapper<SysAccount>()
                .eq(SysAccount::getId, accountId)
                .eq(SysAccount::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUsers(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        // 物理删除账号记录
        accountMapper.delete(new LambdaQueryWrapper<SysAccount>()
                .in(SysAccount::getUserId, ids));
        // 物理删除用户-部门关联记录
        userDeptService.remove(new LambdaQueryWrapper<SysUserDept>()
                .in(SysUserDept::getUserId, ids));
        // 物理删除用户记录
        removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String userId, String newPassword) {
        SysAccount account = accountMapper.selectOne(new LambdaQueryWrapper<SysAccount>()
                .eq(SysAccount::getUserId, userId)
                .eq(SysAccount::getIdentityType, AuthenticationConstants.LoginType.PASSWORD));
        if (account == null) {
            throw new BusinessException(SystemErrorCode.E02008);
        }
        account.setCredential(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        accountMapper.updateById(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String userId, String oldPassword, String newPassword) {
        SysAccount account = accountMapper.selectOne(new LambdaQueryWrapper<SysAccount>()
                .eq(SysAccount::getUserId, userId)
                .eq(SysAccount::getIdentityType, AuthenticationConstants.LoginType.PASSWORD));
        if (account == null) {
            throw new BusinessException(SystemErrorCode.E02008);
        }
        if (!BCrypt.checkpw(oldPassword, account.getCredential())) {
            throw new BusinessException(SystemErrorCode.E02011);
        }
        account.setCredential(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        accountMapper.updateById(account);
    }
}
