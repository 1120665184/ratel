package org.quyq.gwsu.security.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.security.role.domain.SecurityRole;

import java.util.List;

/**
 * 角色 Mapper 接口
 *
 * @author Quyq
 */
@Mapper
public interface SecurityRoleMapper extends BaseMapper<SecurityRole> {

    /**
     * 根据主体ID查询角色列表
     *
     * @param subjectId 主体ID
     * @return 角色列表
     */
    List<SecurityRole> selectRolesBySubjectId(@Param("subjectId") String subjectId);
}
