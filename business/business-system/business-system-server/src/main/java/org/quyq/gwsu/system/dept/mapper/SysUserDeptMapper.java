package org.quyq.gwsu.system.dept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.system.api.dept.vo.UserDeptDetailVO;
import org.quyq.gwsu.system.dept.domain.SysUserDept;

import java.util.List;

/**
 * 用户部门关联 Mapper
 *
 * @author Quyq
 */
public interface SysUserDeptMapper extends BaseMapper<SysUserDept> {

    /**
     * 获取用户所属部门列表
     *
     * @param userId 用户ID
     * @return 用户部门详情列表
     */
    List<UserDeptDetailVO> selectDeptsByUserId(@Param("userId") String userId);
}