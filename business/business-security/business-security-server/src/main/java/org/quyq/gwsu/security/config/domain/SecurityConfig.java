package org.quyq.gwsu.security.config.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.config.vo.ConfigVO;

/**
 * 配置表
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_config")
@Schema(description = "配置表")
public class SecurityConfig extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "值类型：1-基本类型 2-JSON")
    private Integer valueType;

    @Schema(description = "配置类型：1-系统 2-自定义")
    private Integer configType;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "所属模块前缀")
    private String modulePrefix;

    /**
     * VO 转 DO
     *
     * @param vo 配置VO
     * @return SecurityConfig
     */
    public static SecurityConfig toDo(ConfigVO vo) {
        SecurityConfig entity = new SecurityConfig();
        entity.setId(vo.getId());
        entity.setConfigKey(vo.getConfigKey());
        entity.setConfigName(vo.getConfigName());
        entity.setConfigValue(vo.getConfigValue());
        entity.setValueType(vo.getValueType());
        entity.setConfigType(vo.getConfigType());
        entity.setDescription(vo.getDescription());
        entity.setModulePrefix(vo.getModulePrefix());
        return entity;
    }

    /**
     * DO 转 VO
     *
     * @return ConfigVO
     */
    public ConfigVO toVo() {
        ConfigVO vo = new ConfigVO();
        vo.setId(this.id);
        vo.setConfigKey(this.configKey);
        vo.setConfigName(this.configName);
        vo.setConfigValue(this.configValue);
        vo.setValueType(this.valueType);
        vo.setConfigType(this.configType);
        vo.setDescription(this.description);
        vo.setModulePrefix(this.modulePrefix);
        vo.copyBaseProperties(this);
        return vo;
    }

}
