package org.quyq.gwsu.security.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.executor.BatchResult;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.security.api.dict.dto.DictQueryDTO;
import org.quyq.gwsu.security.api.dict.dto.DictSaveDTO;
import org.quyq.gwsu.security.api.dict.vo.DictVO;
import org.quyq.gwsu.common.security.api.vo.DictValueVO;
import org.quyq.gwsu.security.dict.domain.SecurityDict;
import org.quyq.gwsu.security.dict.domain.SecurityDictValue;
import org.quyq.gwsu.security.dict.mapper.SecurityDictMapper;
import org.quyq.gwsu.security.dict.mapper.SecurityDictValueMapper;
import org.quyq.gwsu.security.dict.service.ISecurityDictService;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 字典服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = ISecurityDictService.DICT_DATA_CACHE_PREFIX)
public class SecurityDictServiceImpl extends ServiceImpl<SecurityDictMapper, SecurityDict> implements ISecurityDictService {


    private final SecurityDictValueMapper dictValueMapper;


    @Override
    public DictVO getById(String id) {
        SecurityDict dict = super.getById(id);
        if (dict == null) {
            return null;
        }
        DictVO vo = dict.toVo();
        vo.setValueCount(countDictValues(dict.getDictKey()));
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
            vo.setValueCount(countDictValues(dict.getDictKey()));
            return vo;
        });
    }

    @Cacheable(key = "#dictKey")
    @Override
    public List<DictValueVO> getByDictKey(String dictKey) {
        return dictValueMapper.selectList(
                        new LambdaQueryWrapper<SecurityDictValue>()
                                .eq(SecurityDictValue::getDictKey, dictKey)
                                .eq(SecurityDictValue::getDeleted, false)
                                .orderByAsc(SecurityDictValue::getSort)
                ).stream()
                .map(SecurityDictValue::toVo).toList();
    }

    @CacheEvict(key = "#dto.dictKey")
    @Override
    public Boolean saveOrUpdateDictValue(SecurityDictValue dto) {

        checkCodeValue(dto);

        return dictValueMapper.insertOrUpdate(dto);
    }


    private void checkCodeValue(SecurityDictValue dto) {
        SecurityDict one = getOne(
                new LambdaQueryWrapper<SecurityDict>()
                        .eq(SecurityDict::getDictKey, dto.getDictKey())
        );

        if (Objects.isNull(one)) {
            throw new BusinessException(SecurityErrorCode.E05006);
        }

        SecurityDictValue dictValue = getDictValueByKeyAndValue(dto.getDictKey(), dto.getDictValue());
        if (Objects.nonNull(dictValue)
                && StringUtils.hasText(dto.getId())
                && !dictValue.getId().equals(dto.getId())) {
            throw new BusinessException(SecurityErrorCode.E05007);
        }

    }


    private SecurityDictValue getDictValueByKeyAndValue(String dictKey, String value) {

        return dictValueMapper
                .selectOne(
                        new LambdaQueryWrapper<SecurityDictValue>()
                                .eq(SecurityDictValue::getDictKey, dictKey)
                                .eq(SecurityDictValue::getDictValue, value)
                );

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
                    .eq(SecurityDict::getDeleted, false));
            if (existing != null) {
                throw new BusinessException(SecurityErrorCode.E05004);
            }
            entity.setDictType(2);
        } else {
            // 更新：保留系统字段
            SecurityDict existing = super.getById(dto.getId());
            if (existing == null) {
                throw new BusinessException(SecurityErrorCode.E05006);
            }
            entity.setDictType(existing.getDictType());
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
        List<String> dictKeys = dicts.stream().map(SecurityDict::getDictKey)
                .toList();
        for (String dictKey : dictKeys) {
            SpringUtils.getAopProxy(this)
                    .removeDictValueByKey(dictKey);
        }

        return removeBatchByIds(ids);
    }

    @Override
    @CacheEvict(allEntries = true)
    public Boolean removeDictValueByIds(List<String> ids) {
        return dictValueMapper.deleteByIds(ids) > 0;
    }

    @Override
    @CacheEvict(key = "#dictKey")
    public void removeDictValueByKey(String dictKey) {
        dictValueMapper.delete(
                new LambdaQueryWrapper<SecurityDictValue>()
                        .eq(SecurityDictValue::getDictKey, dictKey)
        );
    }

    @Override
    @CacheEvict(key = "#dictKey")
    @Transactional
    public Boolean updateDictValueSort(String dictKey, List<String> ids) {

        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }

        List<SecurityDictValue> dictValues = dictValueMapper.selectList(
                new LambdaQueryWrapper<SecurityDictValue>()
                        .eq(SecurityDictValue::getDictKey, dictKey)
                        .in(SecurityDictValue::getId, ids)
        );

        if (CollectionUtils.isEmpty(dictValues)) {
            return true;
        }

        // 按照 ids 的顺序设置 sort 字段
        Map<String, Integer> sortMap = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            sortMap.put(ids.get(i), i);
        }

        for (SecurityDictValue dictValue : dictValues) {
            Integer sort = sortMap.get(dictValue.getId());
            if (sort != null) {
                dictValue.setSort(sort);
            }
        }


        List<BatchResult> results = dictValueMapper.insertOrUpdate(dictValues);
        return results != null && !results.isEmpty();
    }

    /**
     * 统计字典值数量
     *
     * @param dictKey 字典ID
     * @return 字典值数量
     */
    private Integer countDictValues(String dictKey) {
        return Math.toIntExact(dictValueMapper.selectCount(
                new LambdaQueryWrapper<SecurityDictValue>()
                        .eq(SecurityDictValue::getDictKey, dictKey)
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
        }
        return wrapper;
    }
}
