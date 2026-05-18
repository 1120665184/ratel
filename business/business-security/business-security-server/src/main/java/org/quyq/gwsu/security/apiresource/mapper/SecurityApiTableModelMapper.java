package org.quyq.gwsu.security.apiresource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiTableModel;

import java.util.List;

/**
 * 接口-表模型绑定 Mapper
 */
public interface SecurityApiTableModelMapper extends BaseMapper<SecurityApiTableModel> {

    /**
     * 通过API_id获取表模型
     * @param apiIds
     * @return
     */
    List<SecurityApiTableModel> listTableModelByApiId(@Param("apiIds") List<String> apiIds);

}
