package org.quyq.gwsu.system.api.common.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

/**
 * @author Quyq
 * @date 2026/5/4
 * @description
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserDTO  extends BaseDTO {


    @Schema(description = "搜索内容（用户名/昵称/手机模糊搜索）")
    private String search;

}
