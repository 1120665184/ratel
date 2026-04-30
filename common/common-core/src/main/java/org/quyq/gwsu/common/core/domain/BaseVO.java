package org.quyq.gwsu.common.core.domain;


import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Quyq
 * @date 2026/4/15
 * @description
 */
@Data
public abstract class BaseVO {

    @Schema(description = "修改人")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String modifyOp;

    @Schema(description = "修改时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modifyTime;

    @Schema(description = "创建人")
    @TableField(fill = FieldFill.INSERT)
    private String createOp;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public <T extends BaseVO> void copyBaseProperties(T source) {
        modifyOp = source.getModifyOp();
        modifyTime = source.getModifyTime();
        createOp = source.getCreateOp();
        createTime = source.getCreateTime();

    }

}
