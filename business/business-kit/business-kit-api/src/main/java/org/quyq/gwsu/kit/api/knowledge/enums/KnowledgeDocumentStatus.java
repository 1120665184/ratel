package org.quyq.gwsu.kit.api.knowledge.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * 知识源文档处理状态。
 */
public enum KnowledgeDocumentStatus {

    UPLOADED,
    PROCESSING,
    PROCESSED,
    FAILED;

    public boolean canTransitTo(KnowledgeDocumentStatus target) {
        Set<KnowledgeDocumentStatus> targets = switch (this) {
            case UPLOADED -> EnumSet.of(PROCESSING);
            case PROCESSING -> EnumSet.of(PROCESSED, FAILED);
            case FAILED -> EnumSet.of(PROCESSING);
            case PROCESSED -> EnumSet.noneOf(KnowledgeDocumentStatus.class);
        };
        return targets.contains(target);
    }
}
