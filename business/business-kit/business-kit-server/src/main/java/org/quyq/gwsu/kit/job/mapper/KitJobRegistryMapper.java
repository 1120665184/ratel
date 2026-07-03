package org.quyq.gwsu.kit.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.job.domain.KitJobRegistry;

import java.util.Date;
import java.util.List;

/**
 * 执行器注册Mapper
 */
@Mapper
public interface KitJobRegistryMapper extends BaseMapper<KitJobRegistry> {

    /**
     * 查询失效的注册ID（DATE_ADD 日期运算）
     */
    List<String> findDead(@Param("timeout") int timeout, @Param("nowTime") Date nowTime);

    /**
     * 查询所有有效的注册信息（DATE_ADD 日期运算）
     */
    List<KitJobRegistry> findAll(@Param("timeout") int timeout, @Param("nowTime") Date nowTime);

    /**
     * 注册保存或更新（ON DUPLICATE KEY / ON CONFLICT）
     */
    int registrySaveOrUpdate(@Param("registryGroup") String registryGroup,
                             @Param("registryKey") String registryKey,
                             @Param("registryValue") String registryValue,
                             @Param("updateTime") Date updateTime);

}
