package org.quyq.gwsu.security.tablemodel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelForeignKeyVO;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelForeignKey;
import org.quyq.gwsu.security.tablemodel.mapper.SecurityTableModelForeignKeyMapper;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelForeignKeyService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 外键约束信息 服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class SecurityTableModelForeignKeyServiceImpl extends ServiceImpl<SecurityTableModelForeignKeyMapper, SecurityTableModelForeignKey>
        implements ISecurityTableModelForeignKeyService {

    @Override
    public TableModelForeignKeyVO getById(String id) {
        SecurityTableModelForeignKey entity = super.getById(id);
        return entity != null ? entity.toVo() : null;
    }

    @Override
    public List<TableModelForeignKeyVO> listByTableId(String tableId) {
        return baseMapper.selectByTableId(tableId).stream()
                .map(SecurityTableModelForeignKey::toVo)
                .toList();
    }

    @Override
    public Boolean saveOrUpdateForeignKey(TableModelForeignKeyVO vo) {
        SecurityTableModelForeignKey entity = SecurityTableModelForeignKey.toDo(vo);
        return saveOrUpdate(entity);
    }

    @Override
    public Boolean saveBatchForeignKeys(List<TableModelForeignKeyVO> foreignKeys) {
        List<SecurityTableModelForeignKey> entities = foreignKeys.stream()
                .map(SecurityTableModelForeignKey::toDo)
                .toList();
        return saveBatch(entities);
    }

    @Override
    public Boolean removeByTableId(String tableId) {
        return remove(new LambdaQueryWrapper<SecurityTableModelForeignKey>()
                .eq(SecurityTableModelForeignKey::getTableId, tableId));
    }

    @Override
    public Boolean removeByIds(List<String> ids) {
        return removeBatchByIds(ids);
    }
}
