package org.quyq.gwsu.security.tablemodel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelColumn;

import java.util.List;

/**
 * 字段详细信息 Mapper 接口
 *
 * @author Quyq
 */
@Mapper
public interface SecurityTableModelColumnMapper extends BaseMapper<SecurityTableModelColumn> {

    /**
     * 根据表ID查询字段列表
     *
     * @param tableId 表ID
     * @return 字段列表
     */
    List<SecurityTableModelColumn> selectByTableId(@Param("tableId") String tableId);
}
