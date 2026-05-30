package org.quyq.gwsu.security.dict.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.security.api.vo.DictValueVO;

/**
 * 字典值表
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_dict_value")
@Schema(description = "字典值表")
public class SecurityDictValue extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "所属字典ID")
    private String dictKey;

    @Schema(description = "字典值")
    private String dictValue;

    @Schema(description = "字典标签")
    private String dictLabel;

    @Schema(description = "排序号")
    private Integer sort;

    /**
     * VO 转 DO
     *
     * @param vo 字典值VO
     * @return SecurityDictValue
     */
    public static SecurityDictValue toDo(DictValueVO vo) {
        SecurityDictValue entity = new SecurityDictValue();
        entity.setId(vo.getId());
        entity.setDictKey(vo.getDictKey());
        entity.setDictLabel(vo.getDictLabel());
        entity.setDictValue(vo.getDictValue());
        entity.setSort(vo.getSort());
        return entity;
    }

    /**
     * DO 转 VO
     *
     * @return DictValueVO
     */
    public DictValueVO toVo() {
        DictValueVO vo = new DictValueVO();
        vo.setId(this.id);
        vo.setDictLabel(this.dictLabel);
        vo.setDictKey(this.dictKey);
        vo.setDictValue(this.dictValue);
        vo.setSort(this.sort);
        vo.copyBaseProperties(this);
        return vo;
    }

}
