package org.quyq.gwsu.security.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.security.api.dict.dto.DictValueSaveDTO;
import org.quyq.gwsu.security.api.dict.vo.DictValueVO;
import org.quyq.gwsu.security.dict.domain.SecurityDictValue;
import org.quyq.gwsu.security.dict.mapper.SecurityDictValueMapper;
import org.quyq.gwsu.security.dict.service.ISecurityDictValueService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典值服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class SecurityDictValueServiceImpl extends ServiceImpl<SecurityDictValueMapper, SecurityDictValue> implements ISecurityDictValueService {

    @Override
    public List<DictValueVO> listByDictId(String dictId) {
        List<SecurityDictValue> values = list(new LambdaQueryWrapper<SecurityDictValue>()
                .eq(SecurityDictValue::getDictId, dictId)
                .eq(SecurityDictValue::getDeleted, false)
                .orderByAsc(SecurityDictValue::getSort));
        return values.stream().map(SecurityDictValue::toVo).toList();
    }

    @Override
    public Boolean saveOrUpdateValue(DictValueSaveDTO dto) {
        SecurityDictValue entity = new SecurityDictValue();
        entity.setId(dto.getId());
        entity.setDictId(dto.getDictId());
        entity.setDictValue(dto.getDictValue());
        entity.setSort(dto.getSort());

        if (dto.getId() == null || dto.getId().isEmpty()) {
            // 新增：如果未指定排序号，则取当前最大排序号+1
            if (dto.getSort() == null) {
                SecurityDictValue maxValue = getOne(new LambdaQueryWrapper<SecurityDictValue>()
                        .eq(SecurityDictValue::getDictId, dto.getDictId())
                        .eq(SecurityDictValue::getDeleted, false)
                        .orderByDesc(SecurityDictValue::getSort)
                        .last("LIMIT 1"));
                entity.setSort(maxValue != null ? maxValue.getSort() + 1 : 1);
            }
        }
        return saveOrUpdate(entity);
    }

    @Override
    public Boolean removeByIds(List<String> ids) {
        return removeBatchByIds(ids);
    }

    @Override
    public Boolean updateSort(List<String> ids) {
        for (int i = 0; i < ids.size(); i++) {
            update(new LambdaUpdateWrapper<SecurityDictValue>()
                    .eq(SecurityDictValue::getId, ids.get(i))
                    .set(SecurityDictValue::getSort, i + 1));
        }
        return true;
    }
}
