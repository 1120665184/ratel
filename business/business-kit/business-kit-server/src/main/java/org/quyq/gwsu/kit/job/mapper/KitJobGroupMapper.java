package org.quyq.gwsu.kit.job.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.job.domain.KitJobGroup;

import java.util.List;

/**
 * 任务组Mapper
 */
@Mapper
public interface KitJobGroupMapper {

    List<KitJobGroup> findAll();

    List<KitJobGroup> findByAddressType(@Param("addressType") int addressType);

    int save(KitJobGroup kitJobGroup);

    int update(KitJobGroup kitJobGroup);

    int remove(@Param("id") int id);

    KitJobGroup load(@Param("id") int id);

    KitJobGroup loadByAppname(@Param("appname") String appname);

    List<KitJobGroup> pageList(@Param("offset") int offset,
                                @Param("pagesize") int pagesize,
                                @Param("appname") String appname,
                                @Param("name") String name);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("appname") String appname,
                      @Param("name") String name);

}
