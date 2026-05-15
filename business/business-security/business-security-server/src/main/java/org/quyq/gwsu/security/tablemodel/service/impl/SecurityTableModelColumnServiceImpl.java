package org.quyq.gwsu.security.tablemodel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelColumnVO;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelColumn;
import org.quyq.gwsu.security.tablemodel.mapper.SecurityTableModelColumnMapper;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelColumnService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字段详细信息 服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class SecurityTableModelColumnServiceImpl extends ServiceImpl<SecurityTableModelColumnMapper, SecurityTableModelColumn>
        implements ISecurityTableModelColumnService {

    @Override
    public TableModelColumnVO getById(String id) {
        SecurityTableModelColumn entity = super.getById(id);
        return entity != null ? entity.toVo() : null;
    }

    @Override
    public List<TableModelColumnVO> listByTableId(String tableId) {
        return baseMapper.selectByTableId(tableId).stream()
                .map(SecurityTableModelColumn::toVo)
                .toList();
    }

    @Override
    public Boolean saveOrUpdateColumn(TableModelColumnVO vo) {
        SecurityTableModelColumn entity = SecurityTableModelColumn.toDo(vo);
        return saveOrUpdate(entity);
    }

    @Override
    public Boolean saveBatchColumns(List<TableModelColumnVO> columns) {
        List<SecurityTableModelColumn> entities = columns.stream()
                .map(SecurityTableModelColumn::toDo)
                .toList();
        return saveBatch(entities);
    }

    @Override
    public Boolean removeByTableId(String tableId) {
        return remove(new LambdaQueryWrapper<SecurityTableModelColumn>()
                .eq(SecurityTableModelColumn::getTableId, tableId));
    }

    @Override
    public Boolean removeByIds(List<String> ids) {
        return removeBatchByIds(ids);
    }
}
