package org.quyq.gwsu.log.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.common.log.vo.LogOperationVO;
import org.quyq.gwsu.log.api.dto.LogOperationQueryDTO;
import org.quyq.gwsu.log.operation.domain.LogOperation;

/**
 * 操作日志 Mapper 接口
 *
 * @author Quyq
 */
public interface LogOperationMapper extends BaseMapper<LogOperation> {

    /**
     * 分页查询操作日志
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<LogOperationVO> selectPageVo(Page<LogOperationVO> page, @Param("query") LogOperationQueryDTO query);
}
