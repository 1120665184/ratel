package org.quyq.gwsu.security.tablemodel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelConfigSaveDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelConfigVO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.tablemodel.domain.SecurityApiTableModelConfig;
import org.quyq.gwsu.security.tablemodel.mapper.SecurityApiTableModelConfigMapper;
import org.quyq.gwsu.security.tablemodel.service.ISecurityApiTableModelConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityApiTableModelConfigServiceImpl extends ServiceImpl<SecurityApiTableModelConfigMapper, SecurityApiTableModelConfig>
        implements ISecurityApiTableModelConfigService {

    @Override
    public IPage<TableModelConfigVO> pageByCondition(TableModelQueryDTO query) {
        Page<SecurityApiTableModelConfig> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SecurityApiTableModelConfig> wrapper = new LambdaQueryWrapper<>();
        if (query.getModulePrefix() != null) {
            wrapper.eq(SecurityApiTableModelConfig::getModulePrefix, query.getModulePrefix());
        }
        if (query.getTableName() != null) {
            wrapper.like(SecurityApiTableModelConfig::getTableName, query.getTableName());
        }
        return page(page, wrapper).convert(SecurityApiTableModelConfig::toVo);
    }

    @Override
    public Boolean saveOrUpdateConfig(TableModelConfigSaveDTO dto) {
        AssertUtils.hasText(dto.getTableName(), SecurityErrorCode.E03001);
        AssertUtils.hasText(dto.getDatasource(), SecurityErrorCode.E03002);

        if (!CollectionUtils.isEmpty(dto.getTableModelIds())) {
            // 批量创建关联配置
            for (String tableModelId : dto.getTableModelIds()) {
                SecurityApiTableModelConfig config = new SecurityApiTableModelConfig();
                config.setTableModelId(tableModelId);
                config.setTableName(dto.getTableName());
                config.setModulePrefix(dto.getModulePrefix());
                config.setDatasource(dto.getDatasource());
                config.setDescription(dto.getDescription());
                save(config);
            }
            return true;
        }

        // 独立表模型
        SecurityApiTableModelConfig config = new SecurityApiTableModelConfig();
        config.setTableName(dto.getTableName());
        config.setModulePrefix(dto.getModulePrefix());
        config.setDatasource(dto.getDatasource());
        config.setDescription(dto.getDescription());
        return save(config);
    }

    @Override
    public TableModelConfigVO getByTableModelId(String tableModelId) {
        SecurityApiTableModelConfig config = getOne(new LambdaQueryWrapper<SecurityApiTableModelConfig>()
                .eq(SecurityApiTableModelConfig::getTableModelId, tableModelId)
                .last("LIMIT 1"));
        return config != null ? config.toVo() : null;
    }

    @Override
    public List<TableModelConfigVO> listIndependent() {
        return list(new LambdaQueryWrapper<SecurityApiTableModelConfig>()
                .isNull(SecurityApiTableModelConfig::getTableModelId))
                .stream()
                .map(SecurityApiTableModelConfig::toVo)
                .toList();
    }
}
