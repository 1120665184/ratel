package org.quyq.gwsu.security.apiresource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiResource;
import org.quyq.gwsu.security.api.apiresource.dto.ApiResourceQueryDTO;
import org.quyq.gwsu.security.api.apiresource.vo.ApiResourceVO;

import java.util.List;

/**
 * 接口资源 Mapper
 *
 * @author Quyq
 */
public interface SecurityApiResourceMapper extends BaseMapper<SecurityApiResource> {

    /**
     * 分页查询
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<ApiResourceVO> selectPageVo(Page<ApiResourceVO> page, @Param("query") ApiResourceQueryDTO query);


    /**
     * 获取指定表模型关联的所有接口资源
     * @param modulePrefix
     * @param datasource
     * @param tableName
     * @return
     */
    List<ApiResourceVO> selectByTableModel(@Param("modulePrefix") String modulePrefix,@Param("datasource") String datasource ,@Param("tableName") String tableName);

}
