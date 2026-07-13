package org.quyq.gwsu.system.dept.dataresource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.authentication.dataresource.DataResourceAttributeProvider;
import org.quyq.gwsu.common.authentication.dataresource.domain.ResourceRuleKeyProperties;
import org.quyq.gwsu.common.authentication.domain.WorkspaceInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.security.enums.DataScope;
import org.quyq.gwsu.system.api.dept.vo.UserDeptDetailVO;
import org.quyq.gwsu.system.dept.domain.SysDept;
import org.quyq.gwsu.system.dept.service.ISysDeptService;
import org.quyq.gwsu.system.dept.service.ISysUserDeptService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Quyq
 * @date 2026/7/13
 * @description 加载用户拥有的部门ID数据资源
 */
@Component
@RequiredArgsConstructor
public class DeptIdDataResourceAttributeProvider implements DataResourceAttributeProvider {

    private final ISysUserDeptService userDeptService;

    private final ISysDeptService deptService;

    @Override
    public ResourceRuleKeyProperties keyInfo() {
        return new ResourceRuleKeyProperties("deptId", "部门ID");
    }

    @Override
    public List<?> datas(WorkspaceInfo workspace, UserInfo userInfo, DataScope dataScope) {
        if (DataScope.ALL == dataScope) {
            return listAllDeptIds();
        }

        List<String> directDeptIds = userDeptService.listDeptsByUser(userInfo.getUserId())
                .stream()
                .map(UserDeptDetailVO::getDeptId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (directDeptIds.isEmpty()) {
            return List.of();
        }

        if (DataScope.DEPT_AND_BELOW == dataScope) {
            return listDeptIdsAndBelow(directDeptIds);
        }

        return directDeptIds;
    }

    private List<String> listAllDeptIds() {
        return deptService.list(new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getDeleted, false))
                .stream()
                .map(SysDept::getId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private List<String> listDeptIdsAndBelow(List<String> directDeptIds) {
        List<SysDept> directDepts = deptService.list(new LambdaQueryWrapper<SysDept>()
                .in(SysDept::getId, directDeptIds)
                .eq(SysDept::getDeleted, false));
        if (directDepts.isEmpty()) {
            return List.of();
        }

        List<String> directPaths = directDepts.stream()
                .map(SysDept::getPath)
                .filter(StringUtils::hasText)
                .toList();
        if (directPaths.isEmpty()) {
            return directDeptIds;
        }

        List<SysDept> allDepts = deptService.list(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeleted, false));
        Set<String> deptIds = new LinkedHashSet<>(directDeptIds);
        for (SysDept dept : allDepts) {
            if (!StringUtils.hasText(dept.getId()) || !StringUtils.hasText(dept.getPath())) {
                continue;
            }
            boolean matched = directPaths.stream()
                    .anyMatch(path -> dept.getPath().equals(path) || dept.getPath().startsWith(path + "/"));
            if (matched) {
                deptIds.add(dept.getId());
            }
        }
        return List.copyOf(deptIds);
    }
}
