package org.quyq.gwsu.security.api.brain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 大脑历史会话批次结果
 *
 * @author Quyq
 */
@Data
@Schema(description = "大脑历史会话批次结果")
public class BrainHistorySessionSliceVo {

    @Schema(description = "当前批次会话列表")
    private List<BrainHistorySessionVo> records;

    @Schema(description = "是否还有更多数据")
    private Boolean hasMore;

    @Schema(description = "下一页页码")
    private Integer nextPageNum;
}
