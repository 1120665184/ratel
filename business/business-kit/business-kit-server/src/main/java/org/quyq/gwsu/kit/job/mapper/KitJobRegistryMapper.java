package org.quyq.gwsu.kit.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.quyq.gwsu.kit.job.domain.KitJobRegistry;

/**
 * 执行器注册Mapper
 */
@Mapper
public interface KitJobRegistryMapper extends BaseMapper<KitJobRegistry> {

}
