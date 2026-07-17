package org.quyq.gwsu.kit.api.knowledge.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * 知识文档导入分析检查点状态。
 */
public enum KnowledgeIngestAnalysisCheckpointStatus {

    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED;

    /**
     * 判断当前状态是否可以迁移至目标状态。
     *
     * @param target 目标状态
     * @return 是否允许迁移
     */
    public boolean canTransitTo(KnowledgeIngestAnalysisCheckpointStatus target) {
        Set<KnowledgeIngestAnalysisCheckpointStatus> targets = switch (this) {
            case PENDING -> EnumSet.of(RUNNING);
            case RUNNING -> EnumSet.of(SUCCEEDED, FAILED);
            case FAILED -> EnumSet.of(PENDING);
            case SUCCEEDED -> EnumSet.noneOf(KnowledgeIngestAnalysisCheckpointStatus.class);
        };
        return targets.contains(target);
    }
}
