package org.quyq.gwsu.security.apiresource.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.security.collector.ApiEndpointCollector;
import org.quyq.gwsu.security.api.apiresource.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.apiresource.vo.TableModelVO;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiTableModel;

import java.util.Collection;
import java.util.List;

/**
 * 接口-表模型绑定服务接口
 */
public interface ISecurityApiTableModelService extends IService<SecurityApiTableModel> {

    /**
     * 分页查询
     */
    IPage<TableModelVO> pageByCondition(TableModelQueryDTO query);

    List<SecurityApiTableModel> listByCondition(TableModelQueryDTO query);

    /**
     * 根据接口资源ID查询表模型列表
     */
    List<TableModelVO> listByApiId(String apiId);

    /**
     * 根据接口资源ID列表批量查询表模型列表（按 modulePrefix+datasource+tableName 去重）
     * 支持大量apiId，内部自动分批查询
     */
    List<TableModelVO> listByApiIds(Collection<String> apiIds);

    /**
     * 根据模块前缀查询表模型列表
     */
    List<TableModelVO> listByModulePrefix(String modulePrefix);

    /**
     * 处理接口-表模型绑定数据（启动时全量覆盖）
     * 由 SecurityApiResourceServiceImpl.handlePermission 在同一事务内调用，不需要独立的分布式锁
     */
    void handleTableModel(String applicationName, ApiEndpointCollector.ApiEndpointWrapper permissions);
}
