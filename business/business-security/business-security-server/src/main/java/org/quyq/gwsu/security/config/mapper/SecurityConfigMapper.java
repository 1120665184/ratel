package org.quyq.gwsu.security.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.quyq.gwsu.security.config.domain.SecurityConfig;

/**
 * 配置 Mapper 接口
 *
 * @author Quyq
 */
@Mapper
public interface SecurityConfigMapper extends BaseMapper<SecurityConfig> {

}
