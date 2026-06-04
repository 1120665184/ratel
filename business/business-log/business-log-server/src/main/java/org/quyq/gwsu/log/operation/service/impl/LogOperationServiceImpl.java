package org.quyq.gwsu.log.operation.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.log.vo.LogOperationVO;
import org.quyq.gwsu.log.api.dto.LogOperationQueryDTO;
import org.quyq.gwsu.log.operation.domain.LogOperation;
import org.quyq.gwsu.log.operation.mapper.LogOperationMapper;
import org.quyq.gwsu.log.operation.service.ILogOperationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 操作日志服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class LogOperationServiceImpl extends ServiceImpl<LogOperationMapper, LogOperation> implements ILogOperationService {

    @Override
    public LogOperationVO getById(String id) {
        LogOperation entity = super.getById(id);
        return entity != null ? entity.toVo() : null;
    }

    @Override
    public IPage<LogOperationVO> pageByCondition(LogOperationQueryDTO query) {
        Page<LogOperationVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return baseMapper.selectPageVo(page, query);
    }

    @Override
    public Boolean saveLog(LogOperationVO vo) {
        LogOperation entity = LogOperation.toDo(vo);
        if(Objects.nonNull(entity.getResponseTime())){
            return updateById(entity);
        }
        return save(entity);
    }


    @Override
    public Boolean removeByIds(List<String> ids) {
        return removeBatchByIds(ids);
    }
}
