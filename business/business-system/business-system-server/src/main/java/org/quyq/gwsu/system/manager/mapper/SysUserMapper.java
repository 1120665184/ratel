package org.quyq.gwsu.system.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.system.api.manager.dto.SysUserQueryDTO;
import org.quyq.gwsu.system.manager.domain.SysUser;

/**
 * 用户 Mapper
 *
 * @author Quyq
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询用户（支持部门筛选，含下级部门）
     */
    IPage<SysUser> selectUserPage(IPage<SysUser> page, @Param("query") SysUserQueryDTO query);
}
