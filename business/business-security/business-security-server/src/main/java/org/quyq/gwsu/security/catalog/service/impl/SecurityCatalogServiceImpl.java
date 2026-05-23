package org.quyq.gwsu.security.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalog;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponent;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponentRef;
import org.quyq.gwsu.security.catalog.mapper.SecurityCatalogComponentMapper;
import org.quyq.gwsu.security.catalog.mapper.SecurityCatalogComponentRefMapper;
import org.quyq.gwsu.security.catalog.mapper.SecurityCatalogMapper;
import org.quyq.gwsu.security.catalog.service.ISecurityCatalogService;
import org.quyq.gwsu.security.catalog.vo.CatalogDefinitionVO;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogComponentVO;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogVO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Catalog服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class SecurityCatalogServiceImpl extends ServiceImpl<SecurityCatalogMapper, SecurityCatalog> implements ISecurityCatalogService {

    private final SecurityCatalogComponentMapper componentMapper;

    private final SecurityCatalogComponentRefMapper refMapper;

    @Override
    public List<SecurityCatalogVO> listAll() {
        List<SecurityCatalog> list = list(new LambdaQueryWrapper<SecurityCatalog>()
                .eq(SecurityCatalog::getDeleted, false)
                .orderByDesc(SecurityCatalog::getActive)
                .orderByDesc(SecurityCatalog::getCreateTime));
        return list.stream()
                .map(SecurityCatalog::toVo)
                .toList();
    }

    @Override
    public SecurityCatalogVO getCatalogById(String id) {
        SecurityCatalog entity = super.getById(id);
        return entity != null ? entity.toVo() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrUpdateCatalog(SecurityCatalogVO vo) {
        AssertUtils.hasText(vo.getCatalogName(), SecurityErrorCode.E05002);

        // 新增时校验 catalogKey 唯一性
        if (vo.getId() == null || vo.getId().isEmpty()) {
            AssertUtils.hasText(vo.getCatalogKey(), SecurityErrorCode.E05001);
            SecurityCatalog existing = getOne(new LambdaQueryWrapper<SecurityCatalog>()
                    .eq(SecurityCatalog::getCatalogKey, vo.getCatalogKey())
                    .eq(SecurityCatalog::getDeleted, false));
            if (existing != null) {
                throw new org.quyq.gwsu.common.core.exception.BusinessException(SecurityErrorCode.E05001);
            }
        } else {
            // 更新时校验 catalogKey 唯一性（排除自身）
            if (vo.getCatalogKey() != null && !vo.getCatalogKey().isEmpty()) {
                SecurityCatalog existing = getOne(new LambdaQueryWrapper<SecurityCatalog>()
                        .eq(SecurityCatalog::getCatalogKey, vo.getCatalogKey())
                        .ne(SecurityCatalog::getId, vo.getId())
                        .eq(SecurityCatalog::getDeleted, false));
                if (existing != null) {
                    throw new org.quyq.gwsu.common.core.exception.BusinessException(SecurityErrorCode.E05001);
                }
            }
        }

        SecurityCatalog entity = SecurityCatalog.toDo(vo);
        saveOrUpdate(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeCatalogs(List<String> ids) {
        // 删除关联的 ref 记录
        for (String catalogId : ids) {
            refMapper.delete(new LambdaQueryWrapper<SecurityCatalogComponentRef>()
                    .eq(SecurityCatalogComponentRef::getCatalogId, catalogId));
        }
        return removeBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean activateCatalog(String id) {
        SecurityCatalog target = super.getById(id);
        AssertUtils.notNull(target, SecurityErrorCode.E05006, "激活Catalog失败，不存在ID为{0}的Catalog", id);

        // 先将所有 Catalog 的 active 设为 0
        update(new LambdaUpdateWrapper<SecurityCatalog>()
                .set(SecurityCatalog::getActive, 0)
                .eq(SecurityCatalog::getDeleted, false));

        // 再将目标 Catalog 的 active 设为 1
        target.setActive(1);
        return updateById(target);
    }

    @Override
    public CatalogDefinitionVO getActiveCatalogDefinition() {
        SecurityCatalog activeCatalog = getOne(new LambdaQueryWrapper<SecurityCatalog>()
                .eq(SecurityCatalog::getActive, 1)
                .eq(SecurityCatalog::getDeleted, false));
        if (activeCatalog == null) {
            return null;
        }
        return buildCatalogDefinition(activeCatalog);
    }

    @Override
    public CatalogDefinitionVO getCatalogDefinitionByKey(String catalogKey) {
        AssertUtils.hasText(catalogKey, SecurityErrorCode.E05001);
        SecurityCatalog catalog = getOne(new LambdaQueryWrapper<SecurityCatalog>()
                .eq(SecurityCatalog::getCatalogKey, catalogKey)
                .eq(SecurityCatalog::getDeleted, false));
        if (catalog == null) {
            return null;
        }
        return buildCatalogDefinition(catalog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean bindComponents(String catalogId, List<String> componentIds) {
        AssertUtils.hasText(catalogId, SecurityErrorCode.E05006);
        SecurityCatalog catalog = super.getById(catalogId);
        AssertUtils.notNull(catalog, SecurityErrorCode.E05006);

        // 先删除该 Catalog 的所有关联记录
        refMapper.delete(new LambdaQueryWrapper<SecurityCatalogComponentRef>()
                .eq(SecurityCatalogComponentRef::getCatalogId, catalogId));

        // 批量插入新的关联记录
        if (!CollectionUtils.isEmpty(componentIds)) {
            List<SecurityCatalogComponentRef> refs = new ArrayList<>();
            for (int i = 0; i < componentIds.size(); i++) {
                SecurityCatalogComponentRef ref = new SecurityCatalogComponentRef();
                ref.setCatalogId(catalogId);
                ref.setComponentId(componentIds.get(i));
                ref.setSortOrder(i);
                refs.add(ref);
            }
            for (SecurityCatalogComponentRef ref : refs) {
                refMapper.insert(ref);
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unbindComponent(String catalogId, String componentId) {
        AssertUtils.hasText(catalogId, SecurityErrorCode.E05006);
        AssertUtils.hasText(componentId, SecurityErrorCode.E05004);
        return refMapper.delete(new LambdaQueryWrapper<SecurityCatalogComponentRef>()
                .eq(SecurityCatalogComponentRef::getCatalogId, catalogId)
                .eq(SecurityCatalogComponentRef::getComponentId, componentId)) > 0;
    }

    @Override
    public List<String> getBoundComponentIds(String catalogId) {
        List<SecurityCatalogComponentRef> refs = refMapper.selectList(
                new LambdaQueryWrapper<SecurityCatalogComponentRef>()
                        .eq(SecurityCatalogComponentRef::getCatalogId, catalogId)
                        .orderByAsc(SecurityCatalogComponentRef::getSortOrder));
        return refs.stream()
                .map(SecurityCatalogComponentRef::getComponentId)
                .toList();
    }

    @Override
    public List<SecurityCatalogComponentVO> getBoundComponents(String catalogId) {
        List<String> componentIds = getBoundComponentIds(catalogId);
        if (CollectionUtils.isEmpty(componentIds)) {
            return List.of();
        }
        List<SecurityCatalogComponent> components = componentMapper.selectBatchIds(componentIds);
        return components.stream()
                .map(SecurityCatalogComponent::toVo)
                .toList();
    }

    // ==================== 私有方法 ====================

    /**
     * 构建 CatalogDefinitionVO
     */
    private CatalogDefinitionVO buildCatalogDefinition(SecurityCatalog catalog) {
        CatalogDefinitionVO definition = new CatalogDefinitionVO();
        definition.setCatalogKey(catalog.getCatalogKey());
        definition.setCatalogName(catalog.getCatalogName());

        // 查询关联的组件ID列表
        List<SecurityCatalogComponentRef> refs = refMapper.selectList(
                new LambdaQueryWrapper<SecurityCatalogComponentRef>()
                        .eq(SecurityCatalogComponentRef::getCatalogId, catalog.getId())
                        .orderByAsc(SecurityCatalogComponentRef::getSortOrder));

        if (CollectionUtils.isEmpty(refs)) {
            definition.setComponents(List.of());
            return definition;
        }

        // 查询组件详情
        List<String> componentIds = refs.stream()
                .map(SecurityCatalogComponentRef::getComponentId)
                .toList();
        List<SecurityCatalogComponent> components = componentMapper.selectBatchIds(componentIds);

        List<CatalogDefinitionVO.ComponentDefinition> componentDefinitions = components.stream()
                .map(comp -> {
                    CatalogDefinitionVO.ComponentDefinition cd = new CatalogDefinitionVO.ComponentDefinition();
                    cd.setComponentName(comp.getComponentName());
                    cd.setDescription(comp.getDescription());
                    cd.setPropsSchema(comp.getPropsSchema());
                    cd.setDefaultProps(comp.getDefaultProps());
                    cd.setCategory(comp.getCategory());
                    return cd;
                })
                .toList();

        definition.setComponents(componentDefinitions);
        return definition;
    }
}
