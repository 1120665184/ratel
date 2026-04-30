package org.quyq.gwsu.security.api.brain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

/**
 * 大脑历史会话查询条件
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "大脑历史会话查询条件")
public class BrainHistoryQueryDTO extends BaseDTO {
    // 继承 BaseDTO 的分页参数：pageNum, pageSize, orderByColumn, asc
}
