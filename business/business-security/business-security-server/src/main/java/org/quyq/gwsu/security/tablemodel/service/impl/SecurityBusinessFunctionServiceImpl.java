package org.quyq.gwsu.security.tablemodel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.quyq.gwsu.security.api.tablemodel.dto.BusinessFunctionQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.BusinessFunctionDetailVO;
import org.quyq.gwsu.security.api.tablemodel.vo.BusinessFunctionVO;
import org.quyq.gwsu.security.tablemodel.domain.SecurityBusinessFunction;
import org.quyq.gwsu.security.tablemodel.domain.SecurityBusinessFunctionTable;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelTable;
import org.quyq.gwsu.security.tablemodel.mapper.SecurityBusinessFunctionMapper;
import org.quyq.gwsu.security.tablemodel.mapper.SecurityBusinessFunctionTableMapper;
import org.quyq.gwsu.security.tablemodel.mapper.SecurityTableModelTableMapper;
import org.quyq.gwsu.security.tablemodel.service.ISecurityBusinessFunctionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityBusinessFunctionServiceImpl
        extends ServiceImpl<SecurityBusinessFunctionMapper, SecurityBusinessFunction>
        implements ISecurityBusinessFunctionService {

    private final SecurityBusinessFunctionTableMapper bfTableMapper;

    private final SecurityTableModelTableMapper tableModelMapper;

    @Override
    public BusinessFunctionVO getById(String id) {
        SecurityBusinessFunction entity = super.getById(id);
        if (entity == null) {
            return null;
        }
        BusinessFunctionVO vo = entity.toVo();
        vo.setTableCount((int) countTablesByBusinessId(id));
        return vo;
    }

    @Override
    public BusinessFunctionDetailVO getDetailById(String id) {
        SecurityBusinessFunction entity = super.getById(id);
        if (entity == null) {
            return null;
        }
        BusinessFunctionDetailVO detail = new BusinessFunctionDetailVO();
        detail.setId(entity.getId());
        detail.setName(entity.getName());
        detail.setSummary(entity.getSummary());
        detail.setDetail(entity.getDetail());
        detail.setSortOrder(entity.getSortOrder());
        detail.copyBaseProperties(entity);

        List<SecurityBusinessFunctionTable> bfTables = bfTableMapper.selectList(
                new LambdaQueryWrapper<SecurityBusinessFunctionTable>()
                        .eq(SecurityBusinessFunctionTable::getBusinessId, id)
                        .eq(SecurityBusinessFunctionTable::getDeleted, false)
                        .orderByAsc(SecurityBusinessFunctionTable::getSortOrder));

        if (!CollectionUtils.isEmpty(bfTables)) {
            List<String> tableModelIds = bfTables.stream()
                    .map(SecurityBusinessFunctionTable::getTableModelId)
                    .toList();
            List<SecurityTableModelTable> tables = tableModelMapper.selectList(
                    new LambdaQueryWrapper<SecurityTableModelTable>()
                            .in(SecurityTableModelTable::getId, tableModelIds)
                            .eq(SecurityTableModelTable::getDeleted, false));
            detail.setTables(tables.stream().map(SecurityTableModelTable::toVo).toList());
        } else {
            detail.setTables(Collections.emptyList());
        }

        return detail;
    }

    @Override
    public List<BusinessFunctionVO> listAll() {
        List<SecurityBusinessFunction> list = list(new LambdaQueryWrapper<SecurityBusinessFunction>()
                .eq(SecurityBusinessFunction::getDeleted, false)
                .orderByAsc(SecurityBusinessFunction::getSortOrder));
        return list.stream().map(entity -> {
            BusinessFunctionVO vo = entity.toVo();
            vo.setTableCount((int) countTablesByBusinessId(entity.getId()));
            return vo;
        }).toList();
    }

    @Override
    public IPage<BusinessFunctionVO> pageByCondition(BusinessFunctionQueryDTO query) {
        Page<SecurityBusinessFunction> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<SecurityBusinessFunction> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(SecurityBusinessFunction::getName, query.getName());
        }
        wrapper.eq(SecurityBusinessFunction::getDeleted, false);
        wrapper.orderByAsc(SecurityBusinessFunction::getSortOrder);
        wrapper.orderByDesc(SecurityBusinessFunction::getCreateTime);

        IPage<SecurityBusinessFunction> result = page(page, wrapper);

        List<String> businessIds = result.getRecords().stream()
                .map(SecurityBusinessFunction::getId)
                .toList();

        Map<String, Long> tableCountMap = Map.of();
        if (!CollectionUtils.isEmpty(businessIds)) {
            List<SecurityBusinessFunctionTable> allBfTables = bfTableMapper.selectList(
                    new LambdaQueryWrapper<SecurityBusinessFunctionTable>()
                            .in(SecurityBusinessFunctionTable::getBusinessId, businessIds)
                            .eq(SecurityBusinessFunctionTable::getDeleted, false));
            tableCountMap = allBfTables.stream()
                    .collect(Collectors.groupingBy(SecurityBusinessFunctionTable::getBusinessId, Collectors.counting()));
        }

        Map<String, Long> finalTableCountMap = tableCountMap;
        return result.convert(entity -> {
            BusinessFunctionVO vo = entity.toVo();
            vo.setTableCount(finalTableCountMap.getOrDefault(entity.getId(), 0L).intValue());
            return vo;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveOrUpdateFunction(BusinessFunctionVO vo) {
        SecurityBusinessFunction entity = SecurityBusinessFunction.toDo(vo);
        boolean saved = saveOrUpdate(entity);

        if (!CollectionUtils.isEmpty(vo.getTableIds())) {
            String businessId = entity.getId();

            bfTableMapper.delete(new LambdaQueryWrapper<SecurityBusinessFunctionTable>()
                    .eq(SecurityBusinessFunctionTable::getBusinessId, businessId));

            List<SecurityBusinessFunctionTable> bfTables = vo.getTableIds().stream()
                    .map(tableId -> {
                        SecurityBusinessFunctionTable bfTable = new SecurityBusinessFunctionTable();
                        bfTable.setBusinessId(businessId);
                        bfTable.setTableModelId(tableId);
                        return bfTable;
                    }).toList();
            for (SecurityBusinessFunctionTable bfTable : bfTables) {
                bfTableMapper.insert(bfTable);
            }
        }

        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }
        bfTableMapper.delete(new LambdaQueryWrapper<SecurityBusinessFunctionTable>()
                .in(SecurityBusinessFunctionTable::getBusinessId, ids));
        return removeBatchByIds(ids);
    }

    private long countTablesByBusinessId(String businessId) {
        return bfTableMapper.selectCount(new LambdaQueryWrapper<SecurityBusinessFunctionTable>()
                .eq(SecurityBusinessFunctionTable::getBusinessId, businessId)
                .eq(SecurityBusinessFunctionTable::getDeleted, false));
    }
}
