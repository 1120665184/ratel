package org.quyq.gwsu.system.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.system.api.dept.dto.RemoveUserDeptDTO;
import org.quyq.gwsu.system.api.dept.dto.SetPrimaryDeptDTO;
import org.quyq.gwsu.system.api.dept.dto.UserDeptSaveDTO;
import org.quyq.gwsu.system.api.dept.vo.UserDeptDetailVO;
import org.quyq.gwsu.system.dept.domain.SysDept;
import org.quyq.gwsu.system.dept.domain.SysUserDept;
import org.quyq.gwsu.system.dept.mapper.SysDeptMapper;
import org.quyq.gwsu.system.dept.mapper.SysUserDeptMapper;
import org.quyq.gwsu.system.dept.service.ISysUserDeptService;
import org.quyq.gwsu.system.errcode.SystemErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 用户部门关联服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class SysUserDeptServiceImpl extends ServiceImpl<SysUserDeptMapper, SysUserDept> implements ISysUserDeptService {

    private final SysDeptMapper deptMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserDept(UserDeptSaveDTO dto) {
        // 校验主部门是否在部门列表中
        if (dto.getPrimaryDeptId() != null) {
            if (CollectionUtils.isEmpty(dto.getDeptIds()) || !dto.getDeptIds().contains(dto.getPrimaryDeptId())) {
                throw new BusinessException(SystemErrorCode.E01013);
            }
        }

        // 删除旧的关联
        remove(new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, dto.getUserId()));

        // 添加新的关联
        boolean hasPrimary = false;
        for (String deptId : dto.getDeptIds()) {
            // 校验部门是否存在
            SysDept dept = deptMapper.selectById(deptId);
            if (dept == null) {
                throw new BusinessException(SystemErrorCode.E01001);
            }

            SysUserDept userDept = new SysUserDept();
            userDept.setUserId(dto.getUserId());
            userDept.setDeptId(deptId);
            userDept.setIsPrimary(dto.getPrimaryDeptId() != null && dto.getPrimaryDeptId().equals(deptId));
            if (userDept.getIsPrimary()) {
                hasPrimary = true;
            }
            save(userDept);
        }

        // 如果没有指定主部门，设置第一个为主部门
        if (!hasPrimary && !CollectionUtils.isEmpty(dto.getDeptIds())) {
            LambdaUpdateWrapper<SysUserDept> updateWrapper = new LambdaUpdateWrapper<SysUserDept>()
                    .eq(SysUserDept::getUserId, dto.getUserId())
                    .eq(SysUserDept::getDeptId, dto.getDeptIds().getFirst())
                    .set(SysUserDept::getIsPrimary, true);
            update(updateWrapper);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPrimaryDept(SetPrimaryDeptDTO dto) {
        // 检查用户是否在该部门
        SysUserDept userDept = getOne(new LambdaQueryWrapper<SysUserDept>()
                .eq(SysUserDept::getUserId, dto.getUserId())
                .eq(SysUserDept::getDeptId, dto.getDeptId()));
        if (userDept == null) {
            throw new BusinessException(SystemErrorCode.E01010);
        }

        // 取消原主部门
        update(new LambdaUpdateWrapper<SysUserDept>()
                .eq(SysUserDept::getUserId, dto.getUserId())
                .eq(SysUserDept::getIsPrimary, true)
                .set(SysUserDept::getIsPrimary, false));

        // 设置新主部门
        userDept.setIsPrimary(true);
        updateById(userDept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserDept(RemoveUserDeptDTO dto) {
        // 获取当前主部门
        SysUserDept currentPrimary = getOne(new LambdaQueryWrapper<SysUserDept>()
                .eq(SysUserDept::getUserId, dto.getUserId())
                .eq(SysUserDept::getIsPrimary, true));

        // 检查是否要移除主部门
        if (currentPrimary != null && dto.getDeptIds().contains(currentPrimary.getDeptId())) {
            if (dto.getNewPrimaryDeptId() == null) {
                throw new BusinessException(SystemErrorCode.E01014);
            }
            // 设置新主部门
            SetPrimaryDeptDTO setPrimaryDTO = new SetPrimaryDeptDTO();
            setPrimaryDTO.setUserId(dto.getUserId());
            setPrimaryDTO.setDeptId(dto.getNewPrimaryDeptId());
            setPrimaryDept(setPrimaryDTO);
        }

        // 删除关联
        remove(new LambdaQueryWrapper<SysUserDept>()
                .eq(SysUserDept::getUserId, dto.getUserId())
                .in(SysUserDept::getDeptId, dto.getDeptIds()));
    }

    @Override
    public List<UserDeptDetailVO> listDeptsByUser(String userId) {
        return baseMapper.selectDeptsByUserId(userId);
    }
}