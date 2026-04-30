package org.quyq.gwsu.common.core.domain;


import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * @author Quyq
 * @date 2026/3/16
 * @description
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public abstract class BaseDO extends BaseVO {

    @Schema(description = "租户ID")
    private String tenantId;

   // @TableLogic
    @Schema(description = "删除标识")
    private Boolean deleted = false;

    @Schema(description = "删除人")
    @TableField(fill = FieldFill.UPDATE)
    private String deleteOp;

    @Schema(description = "删除时间")
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime deleteTime;


    @Override
    public <T extends BaseVO> void copyBaseProperties(T source) {
        super.copyBaseProperties(source);
        if(source instanceof BaseDO baseDO) {
            deleteTime = baseDO.deleteTime;
            deleteOp = baseDO.deleteOp;
            deleted = baseDO.deleted;
            tenantId = baseDO.tenantId;
        }

    }

}
