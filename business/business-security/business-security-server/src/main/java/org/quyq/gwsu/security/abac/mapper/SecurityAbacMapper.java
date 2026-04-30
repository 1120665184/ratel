package org.quyq.gwsu.security.abac.mapper;

import org.quyq.gwsu.security.abac.domain.SecurityAbac;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.quyq.gwsu.security.api.abac.vo.AbacFieldVO;
import org.quyq.gwsu.security.api.abac.vo.AbacPermissionVO;

import java.util.List;

public interface SecurityAbacMapper extends BaseMapper<SecurityAbac> {

    /**
     * 所有启用的表达式访问权限
     *
     * @return
     */
    List<AbacPermissionVO> allAbacPermissions();

    /**
     * 所有启用的表达式字段权限
     * @return
     */
    List<AbacFieldVO>  allAbacFields();

}
