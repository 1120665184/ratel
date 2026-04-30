package org.quyq.gwsu.security.api.brain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大脑历史会话信息
 *
 * @author Quyq
 */
@Data
@Schema(description = "大脑历史会话信息")
public class BrainHistorySessionVo {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "会话标题（第一条消息内容）")
    private String title;

    @Schema(description = "消息数量")
    private Integer messageCount;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "时间显示（几分钟前、几小时前等）")
    private String timeDisplay;
}
