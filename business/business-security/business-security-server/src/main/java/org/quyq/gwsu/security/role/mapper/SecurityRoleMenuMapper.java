package org.quyq.gwsu.security.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenu;

/**
 * 角色菜单关联 Mapper 接口
 *
 * @author Quyq
 */
@Mapper
public interface SecurityRoleMenuMapper extends BaseMapper<SecurityRoleMenu> {

}
