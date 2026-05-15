package org.quyq.gwsu.security.apiresource.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.security.collector.ApiEndpointCollector;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiResource;
import org.quyq.gwsu.security.api.apiresource.dto.ApiResourceQueryDTO;
import org.quyq.gwsu.security.api.apiresource.vo.ApiResourceVO;

import java.util.List;

/**
 * 接口资源服务接口
 *
 * @author Quyq
 */
public interface ISecurityApiResourceService extends IService<SecurityApiResource> {

    /**
     * 根据ID查询
     *
     * @param id 主键ID
     * @return 接口资源信息
     */
    ApiResourceVO getById(Long id);

    /**
     * 根据路径和方法查询
     *
     * @param reqPath   接口地址
     * @param reqMethod 请求方式
     * @return 接口资源信息
     */
    ApiResourceVO getByPathAndMethod(String reqPath, String reqMethod);

    /**
     * 根据模块前缀查询列表
     *
     * @param modulePrefix 模块前缀
     * @return 接口资源列表
     */
    List<ApiResourceVO> listByModulePrefix(String modulePrefix);

    /**
     * 根据Tag名称查询列表
     *
     * @param tagName Tag标签名称
     * @return 接口资源列表
     */
    List<ApiResourceVO> listByTagName(String tagName);

    /**
     * 分页查询
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<ApiResourceVO> pageByCondition(ApiResourceQueryDTO query);

    /**
     * 批量保存或更新
     *
     * @param entities 实体列表
     * @return 是否成功
     */
    Boolean saveOrUpdateBatch(List<SecurityApiResource> entities);

    /**
     * 批量删除
     *
     * @param ids 主键ID列表
     * @return 是否成功
     */
    Boolean removeByIds(List<Long> ids);


    /**
     * 权限列表处理
     *
     * @param applicationName
     * @param permissions
     */
    void handlePermission(String applicationName, ApiEndpointCollector.ApiEndpointWrapper permissions);
}
