package org.quyq.gwsu.security.tablemodel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelForeignKey;

import java.util.List;

/**
 * 外键约束信息 Mapper 接口
 *
 * @author Quyq
 */
@Mapper
public interface SecurityTableModelForeignKeyMapper extends BaseMapper<SecurityTableModelForeignKey> {

    /**
     * 根据表ID查询外键列表
     *
     * @param tableId 表ID
     * @return 外键列表
     */
    List<SecurityTableModelForeignKey> selectByTableId(@Param("tableId") String tableId);
}
