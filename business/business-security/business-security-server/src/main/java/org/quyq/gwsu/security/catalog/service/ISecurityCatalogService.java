package org.quyq.gwsu.security.catalog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalog;
import org.quyq.gwsu.security.catalog.vo.CatalogDefinitionVO;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogVO;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogComponentVO;

import java.util.List;

/**
 * Catalog服务接口
 *
 * @author Quyq
 */
public interface ISecurityCatalogService extends IService<SecurityCatalog> {

    /**
     * 查询所有Catalog列表
     *
     * @return Catalog列表
     */
    List<SecurityCatalogVO> listAll();

    /**
     * 根据ID查询
     *
     * @param id Catalog ID
     * @return Catalog信息
     */
    SecurityCatalogVO getCatalogById(String id);

    /**
     * 新增或更新Catalog
     *
     * @param vo Catalog信息
     * @return Catalog ID
     */
    String saveOrUpdateCatalog(SecurityCatalogVO vo);

    /**
     * 批量删除Catalog
     *
     * @param ids Catalog ID列表
     * @return 是否成功
     */
    Boolean removeCatalogs(List<String> ids);

    /**
     * 激活Catalog（全局唯一，事务保证）
     *
     * @param id Catalog ID
     * @return 是否成功
     */
    Boolean activateCatalog(String id);

    /**
     * 获取当前激活的Catalog完整定义
     *
     * @return Catalog完整定义
     */
    CatalogDefinitionVO getActiveCatalogDefinition();

    /**
     * 根据catalogKey获取Catalog完整定义
     *
     * @param catalogKey Catalog唯一标识
     * @return Catalog完整定义
     */
    CatalogDefinitionVO getCatalogDefinitionByKey(String catalogKey);

    /**
     * 给Catalog绑定组件列表（全量替换）
     *
     * @param catalogId    Catalog ID
     * @param componentIds 组件ID列表
     * @return 是否成功
     */
    Boolean bindComponents(String catalogId, List<String> componentIds);

    /**
     * 解绑Catalog的组件
     *
     * @param catalogId   Catalog ID
     * @param componentId 组件ID
     * @return 是否成功
     */
    Boolean unbindComponent(String catalogId, String componentId);

    /**
     * 获取Catalog已绑定的组件ID列表
     *
     * @param catalogId Catalog ID
     * @return 组件ID列表
     */
    List<String> getBoundComponentIds(String catalogId);

    /**
     * 获取Catalog已绑定的组件详情列表
     *
     * @param catalogId Catalog ID
     * @return 组件详情列表
     */
    List<SecurityCatalogComponentVO> getBoundComponents(String catalogId);
}
