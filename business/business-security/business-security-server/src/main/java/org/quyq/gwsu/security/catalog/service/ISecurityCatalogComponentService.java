package org.quyq.gwsu.security.catalog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponent;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogComponentVO;

import java.util.List;

/**
 * Catalog组件服务接口
 *
 * @author Quyq
 */
public interface ISecurityCatalogComponentService extends IService<SecurityCatalogComponent> {

    /**
     * 查询所有组件列表
     *
     * @return 组件列表
     */
    List<SecurityCatalogComponentVO> listAll();

    /**
     * 根据ID查询
     *
     * @param id 组件ID
     * @return 组件信息
     */
    SecurityCatalogComponentVO getComponentById(String id);

    /**
     * 新增或更新组件
     *
     * @param vo 组件信息
     * @return 组件ID
     */
    String saveOrUpdateComponent(SecurityCatalogComponentVO vo);

    /**
     * 批量删除组件
     *
     * @param ids 组件ID列表
     * @return 是否成功
     */
    Boolean removeComponents(List<String> ids);

    /**
     * 根据ID列表查询组件VO
     *
     * @param ids 组件ID列表
     * @return 组件VO列表
     */
    List<SecurityCatalogComponentVO> listVoByIds(List<String> ids);
}
