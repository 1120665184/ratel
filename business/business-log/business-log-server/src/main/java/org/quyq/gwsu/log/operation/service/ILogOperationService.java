package org.quyq.gwsu.log.operation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.log.vo.LogOperationVO;
import org.quyq.gwsu.log.api.dto.LogOperationQueryDTO;
import org.quyq.gwsu.log.operation.domain.LogOperation;

import java.util.List;

/**
 * 操作日志服务接口
 *
 * @author Quyq
 */
public interface ILogOperationService extends IService<LogOperation> {

    /**
     * 根据ID查询操作日志
     *
     * @param id 日志ID
     * @return 操作日志信息
     */
    LogOperationVO getById(String id);

    /**
     * 分页查询操作日志
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<LogOperationVO> pageByCondition(LogOperationQueryDTO query);

    /**
     * 保存操作日志
     *
     * @param vo 操作日志VO
     * @return 是否成功
     */
    Boolean saveLog(LogOperationVO vo);

    /**
     * 批量删除操作日志
     *
     * @param ids 日志ID列表
     * @return 是否成功
     */
    Boolean removeByIds(List<String> ids);
}
