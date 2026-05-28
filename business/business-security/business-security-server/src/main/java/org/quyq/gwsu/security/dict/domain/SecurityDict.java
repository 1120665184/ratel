package org.quyq.gwsu.security.dict.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.dict.vo.DictVO;

/**
 * 字典表
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_dict")
@Schema(description = "字典表")
public class SecurityDict extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "字典键")
    private String dictKey;

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "字典类型：1-系统 2-自定义")
    private Integer dictType;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "所属模块前缀")
    private String modulePrefix;

    /**
     * VO 转 DO
     *
     * @param vo 字典VO
     * @return SecurityDict
     */
    public static SecurityDict toDo(DictVO vo) {
        SecurityDict entity = new SecurityDict();
        entity.setId(vo.getId());
        entity.setDictKey(vo.getDictKey());
        entity.setDictName(vo.getDictName());
        entity.setDictType(vo.getDictType());
        entity.setDescription(vo.getDescription());
        entity.setModulePrefix(vo.getModulePrefix());
        return entity;
    }

    /**
     * DO 转 VO
     *
     * @return DictVO
     */
    public DictVO toVo() {
        DictVO vo = new DictVO();
        vo.setId(this.id);
        vo.setDictKey(this.dictKey);
        vo.setDictName(this.dictName);
        vo.setDictType(this.dictType);
        vo.setDescription(this.description);
        vo.setModulePrefix(this.modulePrefix);
        vo.copyBaseProperties(this);
        return vo;
    }

}
