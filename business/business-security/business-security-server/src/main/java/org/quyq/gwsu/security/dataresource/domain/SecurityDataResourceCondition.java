package org.quyq.gwsu.security.dataresource.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.security.enums.DataResourceAssertType;
import org.quyq.gwsu.common.security.enums.DataResourceFieldConditionType;
import org.quyq.gwsu.security.api.dataresource.vo.DataResourceConditionVO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 数据资源字段条件配置表
 *
 * @author Quyq
 * @date 2026/4/20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "security_data_resource_condition", autoResultMap = true)
@Schema(description = "数据资源字段条件配置表")
public class SecurityDataResourceCondition extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "数据资源配置ID")
    private String dataResourceId;

    @Schema(description = "字段名")
    private String fieldName;

    @Schema(description = "显示过滤字段为null的数据")
    private Boolean showNull;

    @Schema(description = "关联的用户数据资源字段，多个用逗号分隔")
    private String userResourceFields;

    @Schema(description = "断言类型")
    private DataResourceAssertType assertType;

    @Schema(description = "与上一个条件的关联关系")
    private DataResourceFieldConditionType relationship;

    @Schema(description = "排序号")
    private Integer sort;

    /**
     * 转换为 VO 对象
     */
    public DataResourceConditionVO toVo() {
        DataResourceConditionVO vo = new DataResourceConditionVO();
        vo.setId(this.id);
        vo.setFieldName(this.fieldName);
        vo.setShowNull(this.showNull);
        vo.setUserResourceFields(parseUserResourceFields());
        vo.setAssertType(this.assertType != null ? this.assertType.name() : null);
        vo.setRelationship(this.relationship != null ? this.relationship.name() : null);
        vo.setSort(this.sort);
        vo.copyBaseProperties(this);
        return vo;
    }

    /**
     * 解析用户资源字段列表
     */
    public List<String> parseUserResourceFields() {
        if (this.userResourceFields != null && !this.userResourceFields.isBlank()) {
            return Arrays.asList(this.userResourceFields.split(","));
        }
        return Collections.emptyList();
    }

}
