package org.quyq.gwsu.system.dept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.system.api.dept.vo.DeptTreeVO;
import org.quyq.gwsu.system.api.dept.vo.UserDeptDetailVO;
import org.quyq.gwsu.system.dept.domain.SysDept;

import java.util.List;

/**
 * 部门 Mapper
 *
 * @author Quyq
 */
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 获取部门树
     *
     * @return 部门树列表
     */
    List<DeptTreeVO> selectDeptTree();

    /**
     * 获取部门下的用户列表
     *
     * @param deptId 部门ID
     * @return 用户部门详情列表
     */
    List<UserDeptDetailVO> selectUsersByDeptId(@Param("deptId") String deptId);
}