package org.quyq.gwsu.security.tablemodel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelConfigSaveDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelConfigVO;
import org.quyq.gwsu.security.tablemodel.domain.SecurityApiTableModelConfig;

import java.util.List;

/**
 * 表模型手动配置服务接口
 */
public interface ISecurityApiTableModelConfigService extends IService<SecurityApiTableModelConfig> {

    /**
     * 分页查询
     */
    IPage<TableModelConfigVO> pageByCondition(TableModelQueryDTO query);

    /**
     * 保存或更新配置
     */
    Boolean saveOrUpdateConfig(TableModelConfigSaveDTO dto);

    /**
     * 根据表模型绑定ID查询有效配置
     */
    TableModelConfigVO getByTableModelId(String tableModelId);

    /**
     * 查询独立表模型列表
     */
    List<TableModelConfigVO> listIndependent();
}
