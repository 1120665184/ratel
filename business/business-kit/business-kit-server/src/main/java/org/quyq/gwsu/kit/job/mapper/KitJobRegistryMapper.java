package org.quyq.gwsu.kit.job.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.job.domain.KitJobRegistry;

import java.util.Date;
import java.util.List;

/**
 * 执行器注册Mapper
 */
@Mapper
public interface KitJobRegistryMapper {

    List<Integer> findDead(@Param("timeout") int timeout,
                           @Param("nowTime") Date nowTime);

    int removeDead(@Param("ids") List<Integer> ids);

    List<KitJobRegistry> findAll(@Param("timeout") int timeout,
                                 @Param("nowTime") Date nowTime);

    int registrySaveOrUpdate(@Param("registryGroup") String registryGroup,
                             @Param("registryKey") String registryKey,
                             @Param("registryValue") String registryValue,
                             @Param("updateTime") Date updateTime);

    int registryDelete(@Param("registryGroup") String registryGroup,
                       @Param("registryKey") String registryKey,
                       @Param("registryValue") String registryValue);

    int removeByRegistryGroupAndKey(@Param("registryGroup") String registryGroup,
                                    @Param("registryKey") String registryKey);

}
