package org.quyq.gwsu.system.dept.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.system.api.dept.enums.DeptTypeEnum;
import org.quyq.gwsu.system.api.dept.vo.DeptVO;

/**
 * 部门表
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "sys_dept", autoResultMap = true)
@Schema(description = "部门表")
public class SysDept extends BaseDO {

    public static final String ROOT_PARENT_ID = "0";

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "部门名称")
    private String name;

    @Schema(description = "部门类型")
    private DeptTypeEnum type;

    @Schema(description = "主父部门ID")
    private String parentId;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "层级路径")
    private String path;

    /**
     * DO 转 VO
     *
     * @return DeptVO
     */
    public DeptVO toVo() {
        DeptVO vo = new DeptVO();
        vo.setId(this.id);
        vo.setName(this.name);
        vo.setType(this.type);
        vo.setParentId(this.parentId);
        vo.setEnabled(this.enabled);
        vo.setSort(this.sort);
        vo.setPath(this.path);
        vo.copyBaseProperties(this);
        return vo;
    }
}