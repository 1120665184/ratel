package org.quyq.gwsu.security.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponent;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponentRef;
import org.quyq.gwsu.security.catalog.mapper.SecurityCatalogComponentMapper;
import org.quyq.gwsu.security.catalog.mapper.SecurityCatalogComponentRefMapper;
import org.quyq.gwsu.security.catalog.service.ISecurityCatalogComponentService;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogComponentVO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * Catalog组件服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class SecurityCatalogComponentServiceImpl extends ServiceImpl<SecurityCatalogComponentMapper, SecurityCatalogComponent> implements ISecurityCatalogComponentService {

    private final SecurityCatalogComponentRefMapper refMapper;

    @Override
    public List<SecurityCatalogComponentVO> listAll() {
        List<SecurityCatalogComponent> list = list(new LambdaQueryWrapper<SecurityCatalogComponent>()
                .eq(SecurityCatalogComponent::getDeleted, false)
                .orderByAsc(SecurityCatalogComponent::getSortOrder));
        return list.stream()
                .map(SecurityCatalogComponent::toVo)
                .toList();
    }

    @Override
    public SecurityCatalogComponentVO getComponentById(String id) {
        SecurityCatalogComponent entity = super.getById(id);
        return entity != null ? entity.toVo() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrUpdateComponent(SecurityCatalogComponentVO vo) {
        AssertUtils.hasText(vo.getDescription(), SecurityErrorCode.E05004);
        AssertUtils.hasText(vo.getPropsSchema(), SecurityErrorCode.E05005);

        // 新增时校验 componentName 唯一性
        if (vo.getId() == null || vo.getId().isEmpty()) {
            AssertUtils.hasText(vo.getComponentName(), SecurityErrorCode.E05003);
            SecurityCatalogComponent existing = getOne(new LambdaQueryWrapper<SecurityCatalogComponent>()
                    .eq(SecurityCatalogComponent::getComponentName, vo.getComponentName())
                    .eq(SecurityCatalogComponent::getDeleted, false));
            if (existing != null) {
                throw new org.quyq.gwsu.common.core.exception.BusinessException(SecurityErrorCode.E05003);
            }
        } else {
            // 更新时校验 componentName 唯一性（排除自身）
            if (vo.getComponentName() != null && !vo.getComponentName().isEmpty()) {
                SecurityCatalogComponent existing = getOne(new LambdaQueryWrapper<SecurityCatalogComponent>()
                        .eq(SecurityCatalogComponent::getComponentName, vo.getComponentName())
                        .ne(SecurityCatalogComponent::getId, vo.getId())
                        .eq(SecurityCatalogComponent::getDeleted, false));
                if (existing != null) {
                    throw new org.quyq.gwsu.common.core.exception.BusinessException(SecurityErrorCode.E05003);
                }
            }
        }

        SecurityCatalogComponent entity = SecurityCatalogComponent.toDo(vo);
        saveOrUpdate(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeComponents(List<String> ids) {
        // 删除关联的 ref 记录
        for (String componentId : ids) {
            refMapper.delete(new LambdaQueryWrapper<SecurityCatalogComponentRef>()
                    .eq(SecurityCatalogComponentRef::getComponentId, componentId));
        }
        return removeBatchByIds(ids);
    }

    @Override
    public List<SecurityCatalogComponentVO> listVoByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<SecurityCatalogComponent> components = super.listByIds(ids);
        return components.stream()
                .map(SecurityCatalogComponent::toVo)
                .toList();
    }
}
