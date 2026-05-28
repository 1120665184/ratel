package org.quyq.gwsu.security.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.security.api.dict.dto.DictQueryDTO;
import org.quyq.gwsu.security.api.dict.dto.DictSaveDTO;
import org.quyq.gwsu.security.api.dict.vo.DictVO;
import org.quyq.gwsu.security.dict.domain.SecurityDict;
import org.quyq.gwsu.security.dict.domain.SecurityDictValue;
import org.quyq.gwsu.security.dict.mapper.SecurityDictMapper;
import org.quyq.gwsu.security.dict.mapper.SecurityDictValueMapper;
import org.quyq.gwsu.security.dict.service.ISecurityDictService;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class SecurityDictServiceImpl extends ServiceImpl<SecurityDictMapper, SecurityDict> implements ISecurityDictService {

    private static final String DEFAULT_MODULE_PREFIX = "security";

    private final SecurityDictValueMapper dictValueMapper;

    @Override
    public DictVO getById(String id) {
        SecurityDict dict = super.getById(id);
        if (dict == null) {
            return null;
        }
        DictVO vo = dict.toVo();
        vo.setValueCount(countDictValues(dict.getId()));
        return vo;
    }

    @Override
    public IPage<DictVO> pageByCondition(DictQueryDTO query) {
        LambdaQueryWrapper<SecurityDict> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc(SecurityDict::getModifyTime);

        Page<SecurityDict> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<SecurityDict> dictPage = page(page, wrapper);

        return dictPage.convert(dict -> {
            DictVO vo = dict.toVo();
            vo.setValueCount(countDictValues(dict.getId()));
            return vo;
        });
    }

    @Override
    public Boolean saveOrUpdateDict(DictSaveDTO dto) {
        SecurityDict entity = new SecurityDict();
        entity.setId(dto.getId());
        entity.setDictKey(dto.getDictKey());
        entity.setDictName(dto.getDictName());
        entity.setDescription(dto.getDescription());

        if (dto.getId() == null || dto.getId().isEmpty()) {
            // 新增：校验字典键唯一性
            SecurityDict existing = getOne(new LambdaQueryWrapper<SecurityDict>()
                    .eq(SecurityDict::getDictKey, dto.getDictKey())
                    .eq(SecurityDict::getModulePrefix, DEFAULT_MODULE_PREFIX)
                    .eq(SecurityDict::getDeleted, false));
            if (existing != null) {
                throw new BusinessException(SecurityErrorCode.E05004);
            }
            entity.setDictType(2);
            entity.setModulePrefix(DEFAULT_MODULE_PREFIX);
        } else {
            // 更新：保留系统字段
            SecurityDict existing = super.getById(dto.getId());
            if (existing == null) {
                throw new BusinessException(SecurityErrorCode.E05006);
            }
            entity.setDictType(existing.getDictType());
            entity.setModulePrefix(existing.getModulePrefix());
            // 系统字典不可修改字典键
            if (existing.getDictType() == 1 && !existing.getDictKey().equals(dto.getDictKey())) {
                entity.setDictKey(existing.getDictKey());
            }
        }
        return saveOrUpdate(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeByIds(List<String> ids) {
        List<SecurityDict> dicts = listByIds(ids);
        for (SecurityDict dict : dicts) {
            if (dict.getDictType() == 1) {
                throw new BusinessException(SecurityErrorCode.E05005);
            }
        }

        // 删除字典关联的字典值
        for (String dictId : ids) {
            List<SecurityDictValue> values = dictValueMapper.selectList(
                    new LambdaQueryWrapper<SecurityDictValue>()
                            .eq(SecurityDictValue::getDictId, dictId));
            if (!values.isEmpty()) {
                dictValueMapper.deleteByIds(values.stream().map(SecurityDictValue::getId).toList());
            }
        }

        return removeBatchByIds(ids);
    }

    /**
     * 统计字典值数量
     *
     * @param dictId 字典ID
     * @return 字典值数量
     */
    private Integer countDictValues(String dictId) {
        return Math.toIntExact(dictValueMapper.selectCount(
                new LambdaQueryWrapper<SecurityDictValue>()
                        .eq(SecurityDictValue::getDictId, dictId)
                        .eq(SecurityDictValue::getDeleted, false)));
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<SecurityDict> buildQueryWrapper(DictQueryDTO query) {
        LambdaQueryWrapper<SecurityDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecurityDict::getDeleted, false);

        if (query != null) {
            if (query.getDictKey() != null && !query.getDictKey().isEmpty()) {
                wrapper.like(SecurityDict::getDictKey, query.getDictKey());
            }
            if (query.getDictName() != null && !query.getDictName().isEmpty()) {
                wrapper.like(SecurityDict::getDictName, query.getDictName());
            }
            if (query.getDictType() != null) {
                wrapper.eq(SecurityDict::getDictType, query.getDictType());
            }
            if (query.getModulePrefix() != null && !query.getModulePrefix().isEmpty()) {
                wrapper.eq(SecurityDict::getModulePrefix, query.getModulePrefix());
            }
        }
        return wrapper;
    }
}
