package org.quyq.gwsu.security.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.menu.domain.SecurityMenu;

import java.util.List;

/**
 * 菜单 Mapper 接口
 *
 * @author Quyq
 */
@Mapper
public interface SecurityMenuMapper extends BaseMapper<SecurityMenu> {

    /**
     * 根据主体ID查询菜单列表
     *
     * @param subjectId 主体ID
     * @param owner 菜单所属类型
     * @return 菜单列表
     */
    List<SecurityMenu> selectMenusBySubjectId(@Param("subjectId") String subjectId, @Param("owner") MenuOwner owner);

    /**
     * 根据角色编码列表查询菜单列表
     *
     * @param roleCodes 角色编码列表
     * @param owner 菜单所属类型
     * @return 菜单列表
     */
    List<SecurityMenu> selectMenusByRoleCodes(@Param("roleCodes") List<String> roleCodes, @Param("owner") MenuOwner owner);
}
