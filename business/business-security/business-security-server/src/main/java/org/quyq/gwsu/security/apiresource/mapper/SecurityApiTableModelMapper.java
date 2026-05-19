package org.quyq.gwsu.security.apiresource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.security.api.apiresource.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiTableModel;

import java.util.List;

/**
 * 接口-表模型绑定 Mapper
 */
public interface SecurityApiTableModelMapper extends BaseMapper<SecurityApiTableModel> {

    /**
     * 通过条件分页查询
     * @param page
     * @param query
     * @return
     */
    IPage<SecurityApiTableModel> pageByCondition(IPage<SecurityApiTableModel> page,@Param("param") TableModelQueryDTO query);
    /**
     * 通过条件查询接口资源绑定的表模型
     * @param param
     * @return
     */
    List<SecurityApiTableModel> listTableModelByCondition(@Param("param")TableModelQueryDTO param);
    /**
     * 通过API_id获取表模型
     * @param apiIds
     * @return
     */
    List<SecurityApiTableModel> listTableModelByApiId(@Param("apiIds") List<String> apiIds);

}
