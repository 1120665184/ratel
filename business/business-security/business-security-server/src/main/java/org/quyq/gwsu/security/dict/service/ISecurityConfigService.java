package org.quyq.gwsu.security.dict.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.api.config.dto.ConfigQueryDTO;
import org.quyq.gwsu.security.api.config.dto.ConfigSaveDTO;
import org.quyq.gwsu.common.security.api.vo.ConfigVO;
import org.quyq.gwsu.security.dict.domain.SecurityConfig;

import java.util.List;
import java.util.Map;

/**
 * 配置服务接口
 *
 * @author Quyq
 */
public interface ISecurityConfigService extends IService<SecurityConfig> {

    String CACHE_CONFIG_PREFIX = "config";

    /**
     * 根据ID查询配置
     *
     * @param id 配置ID
     * @return 配置信息
     */
    ConfigVO getById(String id);

    /**
     * 根据配置键查询配置
     *
     * @param configKey    配置键
     * @return 配置信息
     */
    ConfigVO getByKey(String configKey);


    Map<String , ConfigVO> getByKeys(List<String> keys);

    /**
     * 分页查询配置
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<ConfigVO> pageByCondition(ConfigQueryDTO query);

    /**
     * 新增或更新配置
     *
     * @param dto 配置保存请求
     * @return 是否成功
     */
    Boolean saveOrUpdateConfig(ConfigSaveDTO dto);

    /**
     * 批量删除配置
     *
     * @param ids 配置ID列表
     * @return 是否成功
     */
    Boolean removeByIds(List<String> ids);
}
