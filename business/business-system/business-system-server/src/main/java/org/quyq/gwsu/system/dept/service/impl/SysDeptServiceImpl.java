package org.quyq.gwsu.system.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.system.api.dept.dto.DeptSaveDTO;
import org.quyq.gwsu.system.api.dept.vo.DeptTreeVO;
import org.quyq.gwsu.system.api.dept.vo.DeptVO;
import org.quyq.gwsu.system.api.dept.vo.UserDeptDetailVO;
import org.quyq.gwsu.system.dept.domain.SysDept;
import org.quyq.gwsu.system.dept.domain.SysDeptParent;
import org.quyq.gwsu.system.dept.domain.SysUserDept;
import org.quyq.gwsu.system.dept.mapper.SysDeptMapper;
import org.quyq.gwsu.system.dept.mapper.SysDeptParentMapper;
import org.quyq.gwsu.system.dept.mapper.SysUserDeptMapper;
import org.quyq.gwsu.system.dept.service.ISysDeptService;
import org.quyq.gwsu.system.errcode.SystemErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {

    private final SysDeptParentMapper deptParentMapper;
    private final SysUserDeptMapper userDeptMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveDept(DeptSaveDTO dto) {
        // 校验部门名称是否重复
        LambdaQueryWrapper<SysDept> nameWrapper = new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getName, dto.getName());
        if (dto.getId() != null) {
            nameWrapper.ne(SysDept::getId, dto.getId());
        }
        if (exists(nameWrapper)) {
            throw new BusinessException(SystemErrorCode.E01002);
        }

        SysDept dept;
        if (dto.getId() != null) {
            // 更新
            dept = getById(dto.getId());
            if (dept == null) {
                throw new BusinessException(SystemErrorCode.E01001);
            }
            // 部门类型不可修改
            if (!dept.getType().equals(dto.getType())) {
                throw new BusinessException(SystemErrorCode.E01008);
            }
            // 校验父部门
            validateParent(dto.getId(), dto.getParentId());
            dept.setName(dto.getName());
            dept.setParentId(dto.getParentId());
            dept.setEnabled(dto.getEnabled());
            dept.setSort(dto.getSort());
            // 重新计算 path
            dept.setPath(calculatePath(dto.getParentId(), dept.getId()));
            updateById(dept);
        } else {
            // 新增
            dept = new SysDept();
            dept.setName(dto.getName());
            dept.setType(dto.getType());
            dept.setParentId(dto.getParentId());
            dept.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
            dept.setSort(dto.getSort() != null ? dto.getSort() : 0);
            // 先保存获取ID
            save(dept);
            // 计算并更新 path
            dept.setPath(calculatePath(dto.getParentId(), dept.getId()));
            updateById(dept);
        }

        // 处理额外父部门
        if (!CollectionUtils.isEmpty(dto.getExtraParentIds())) {
            // 删除旧的额外父部门
            deptParentMapper.delete(new LambdaQueryWrapper<SysDeptParent>()
                    .eq(SysDeptParent::getDeptId, dept.getId()));
            // 添加新的额外父部门
            for (String parentId : dto.getExtraParentIds()) {
                if (parentId.equals(dept.getId())) {
                    throw new BusinessException(SystemErrorCode.E01006);
                }
                SysDept parent = getById(parentId);
                if (parent == null) {
                    throw new BusinessException(SystemErrorCode.E01005);
                }
                SysDeptParent deptParent = new SysDeptParent();
                deptParent.setDeptId(dept.getId());
                deptParent.setParentId(parentId);
                deptParentMapper.insert(deptParent);
            }
        }

        return dept.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDept(String id) {
        SysDept dept = getById(id);
        if (dept == null) {
            throw new BusinessException(SystemErrorCode.E01001);
        }

        // 检查是否存在子部门
        long childCount = count(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(SystemErrorCode.E01003);
        }

        // 检查是否存在关联用户
        long userCount = userDeptMapper.selectCount(new LambdaQueryWrapper<SysUserDept>()
                .eq(SysUserDept::getDeptId, id));
        if (userCount > 0) {
            throw new BusinessException(SystemErrorCode.E01004);
        }

        // 删除额外父部门关系
        deptParentMapper.delete(new LambdaQueryWrapper<SysDeptParent>()
                .eq(SysDeptParent::getDeptId, id));
        deptParentMapper.delete(new LambdaQueryWrapper<SysDeptParent>()
                .eq(SysDeptParent::getParentId, id));

        // 删除部门
        removeById(id);
    }

    @Override
    public DeptVO getDeptDetail(String id) {
        SysDept dept = getById(id);
        if (dept == null) {
            throw new BusinessException(SystemErrorCode.E01001);
        }
        DeptVO vo = dept.toVo();

        // 获取所有父部门ID
        List<String> parentIds = new ArrayList<>();
        if (dept.getParentId() != null) {
            parentIds.add(dept.getParentId());
            // 填充主父部门名称
            SysDept parent = getById(dept.getParentId());
            if (parent != null) {
                vo.setParentName(parent.getName());
            }
        }
        // 填充额外父部门（带名称）
        List<SysDeptParent> deptParents = deptParentMapper.selectList(
                new LambdaQueryWrapper<SysDeptParent>().eq(SysDeptParent::getDeptId, id));
        List<DeptVO.ExtraParentVO> extraParents = new ArrayList<>();
        for (SysDeptParent ep : deptParents) {
            parentIds.add(ep.getParentId());
            SysDept parent = getById(ep.getParentId());
            if (parent != null) {
                DeptVO.ExtraParentVO epVo = new DeptVO.ExtraParentVO();
                epVo.setId(parent.getId());
                epVo.setName(parent.getName());
                extraParents.add(epVo);
            }
        }
        vo.setParentIds(parentIds);
        vo.setExtraParents(extraParents);

        return vo;
    }

    @Override
    public List<DeptTreeVO> getDeptTree() {
        List<DeptTreeVO> allDepts = baseMapper.selectDeptTree();
        return buildDeptTree(allDepts);
    }

    @Override
    public List<DeptVO> listChildren(String parentId) {
        return list(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, parentId)
                .orderByAsc(SysDept::getSort)
                .orderByAsc(SysDept::getId))
                .stream()
                .map(SysDept::toVo)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addParent(String id, String parentId) {
        SysDept dept = getById(id);
        if (dept == null) {
            throw new BusinessException(SystemErrorCode.E01001);
        }
        SysDept parent = getById(parentId);
        if (parent == null) {
            throw new BusinessException(SystemErrorCode.E01005);
        }
        if (id.equals(parentId)) {
            throw new BusinessException(SystemErrorCode.E01006);
        }

        // 检查是否已存在
        Long count = deptParentMapper.selectCount(new LambdaQueryWrapper<SysDeptParent>()
                .eq(SysDeptParent::getDeptId, id)
                .eq(SysDeptParent::getParentId, parentId));
        if (count > 0) {
            return;
        }

        SysDeptParent deptParent = new SysDeptParent();
        deptParent.setDeptId(id);
        deptParent.setParentId(parentId);
        deptParentMapper.insert(deptParent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeParent(String id, String parentId) {
        SysDept dept = getById(id);
        if (dept == null) {
            throw new BusinessException(SystemErrorCode.E01001);
        }

        // 不能移除主父部门
        if (parentId.equals(dept.getParentId())) {
            throw new BusinessException(SystemErrorCode.E01009);
        }

        deptParentMapper.delete(new LambdaQueryWrapper<SysDeptParent>()
                .eq(SysDeptParent::getDeptId, id)
                .eq(SysDeptParent::getParentId, parentId));
    }

    @Override
    public List<UserDeptDetailVO> listUsersByDept(String deptId) {
        return baseMapper.selectUsersByDeptId(deptId);
    }

    @Override
    public Map<String, Long> countUsersByDept() {
        List<SysUserDept> allUserDepts = userDeptMapper.selectList(
                new LambdaQueryWrapper<SysUserDept>());
        return allUserDepts.stream()
                .collect(Collectors.groupingBy(SysUserDept::getDeptId, Collectors.counting()));
    }

    /**
     * 校验父部门
     */
    private void validateParent(String deptId, String parentId) {
        if (parentId == null) {
            return;
        }
        if (deptId.equals(parentId)) {
            throw new BusinessException(SystemErrorCode.E01006);
        }
        SysDept parent = getById(parentId);
        if (parent == null) {
            throw new BusinessException(SystemErrorCode.E01005);
        }
        // 检查循环引用：路径包含当前ID即为循环
        if (parent.getPath() != null) {
            String targetIdStr = String.valueOf(deptId);
            if (parent.getPath().equals(targetIdStr) ||
                parent.getPath().startsWith(targetIdStr + "/") ||
                parent.getPath().contains("/" + targetIdStr + "/") ||
                parent.getPath().endsWith("/" + targetIdStr)) {
                throw new BusinessException(SystemErrorCode.E01007);
            }
        }
    }

    /**
     * 计算层级路径
     * 格式：1/2048589287119335424（无前后斜杠）
     */
    private String calculatePath(String parentId, String currentId) {
        if (parentId == null) {
            return String.valueOf(currentId);
        }
        SysDept parent = getById(parentId);
        if (parent == null) {
            return String.valueOf(currentId);
        }
        // parent.getPath() 格式为 1/xxx，直接拼接新 ID
        return parent.getPath() + "/" + currentId;
    }

    /**
     * 构建部门树
     */
    private List<DeptTreeVO> buildDeptTree(List<DeptTreeVO> allDepts) {
        if (allDepts == null || allDepts.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<DeptTreeVO>> groupByParent = allDepts.stream()
                .collect(Collectors.groupingBy(
                        vo -> vo.getParentId() != null ? vo.getParentId() : SysDept.ROOT_PARENT_ID
                ));

        List<DeptTreeVO> roots = groupByParent.getOrDefault(SysDept.ROOT_PARENT_ID, new ArrayList<>());
        roots.forEach(root -> fillChildren(root, groupByParent));

        return roots;
    }

    /**
     * 递归填充子部门
     */
    private void fillChildren(DeptTreeVO parent, Map<String, List<DeptTreeVO>> groupByParent) {
        List<DeptTreeVO> children = groupByParent.get(parent.getId());
        if (children != null && !children.isEmpty()) {
            parent.setChildren(children);
            children.forEach(child -> fillChildren(child, groupByParent));
        }
    }
}