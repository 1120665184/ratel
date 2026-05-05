package org.quyq.gwsu.security.dataresource.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.security.domain.DataResoureRule;
import org.quyq.gwsu.security.api.dataresource.dto.DataResourceQueryDTO;
import org.quyq.gwsu.security.api.dataresource.dto.DataResourceSaveDTO;
import org.quyq.gwsu.security.api.dataresource.vo.DataResourceVO;
import org.quyq.gwsu.security.dataresource.domain.SecurityDataResource;

import java.util.List;

/**
 * 数据资源配置服务接口
 *
 * @author Quyq
 * @date 2026/4/20
 */
public interface ISecurityDataResourceService extends IService<SecurityDataResource> {

    /**
     * 根据ID查询数据资源配置
     *
     * @param id 主键ID
     * @return 数据资源配置VO
     */
    DataResourceVO getById(Long id);

    /**
     * 根据表名查询数据资源配置列表
     *
     * @param tableName 表名
     * @return 数据资源配置列表
     */
    List<DataResourceVO> listByTableName(String tableName);

    /**
     * 分页查询数据资源配置
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<DataResourceVO> pageByCondition(DataResourceQueryDTO query);

    /**
     * 新增或更新数据资源配置
     *
     * @param dto 数据资源配置DTO
     * @return 是否成功
     */
    Boolean saveOrUpdate(DataResourceSaveDTO dto);


    /**
     * 获取所有启用的数据资源规则（用于Redis同步）
     *
     * @return 数据资源规则列表
     */
    List<DataResoureRule> getAllEnabledRules();

    /**
     * 同步数据资源规则到 Redis
     *
     * @return 是否成功
     */
    Boolean syncToRedis();

}
