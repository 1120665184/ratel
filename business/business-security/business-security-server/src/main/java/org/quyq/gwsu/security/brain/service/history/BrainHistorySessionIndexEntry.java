package org.quyq.gwsu.security.brain.service.history;

import lombok.Data;

/**
 * 历史会话 Redis 索引明细。
 */
@Data
public class BrainHistorySessionIndexEntry {

    private String sessionId;

    private String title;

    private Integer messageCount;

    private String updatedAt;

    private String logPath;
}
