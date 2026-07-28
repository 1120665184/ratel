package org.quyq.gwsu.security.brain.service.history;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.DistributedStore;
import io.agentscope.harness.agent.IsolationScope;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.model.FileInfo;
import io.agentscope.harness.agent.filesystem.model.GlobResult;
import io.agentscope.harness.agent.filesystem.model.ReadResult;
import io.agentscope.harness.agent.filesystem.spec.RemoteFilesystemSpec;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 基于 AgentScope BaseStore 的历史会话仓储。
 */
@Repository
@Slf4j
public class BrainHistoryBaseStoreRepository {

    private static final String SESSION_LOG_GLOB = "*.log.jsonl";
    private static final String AGENTS_ROOT = "agents";
    private static final String SESSION_DIR = "sessions";
    private static final String SESSION_ROUTE = AGENTS_ROOT + "/" + CoreConstants.Agent.BRAIN_AGENT_NAME + "/" + SESSION_DIR;

    private final AbstractFilesystem filesystem;

    public BrainHistoryBaseStoreRepository(DistributedStore distributedStore) {
        Path workspace = Path.of(System.getProperty("java.io.tmpdir"), "ratel-brain-history");
        this.filesystem = new RemoteFilesystemSpec(distributedStore.baseStore())
                .isolationScope(IsolationScope.USER)
                .toFilesystem(workspace, CoreConstants.Agent.BRAIN_AGENT_NAME, null);
    }

    public List<FileInfo> listSessionLogs(String userId) {
        RuntimeContext runtimeContext = runtimeContext(userId);
        GlobResult result = filesystem.glob(runtimeContext, SESSION_LOG_GLOB, SESSION_ROUTE);
        if (log.isDebugEnabled()) {
            List<String> matchedPaths = result != null && result.matches() != null
                    ? result.matches().stream()
                    .filter(Objects::nonNull)
                    .map(FileInfo::path)
                    .filter(Objects::nonNull)
                    .toList()
                    : List.of();
            log.debug("查询历史会话日志: userId={}, basePath={}, pattern={}, success={}, matchedPaths={}",
                    userId,
                    SESSION_ROUTE,
                    SESSION_LOG_GLOB,
                    result != null && result.isSuccess(),
                    matchedPaths);
        }
        if (result == null || !result.isSuccess() || CollectionUtils.isEmpty(result.matches())) {
            return List.of();
        }
        return result.matches().stream()
                .filter(Objects::nonNull)
                .filter(fileInfo -> !fileInfo.isDirectory())
                .filter(fileInfo -> fileInfo.path() != null && !fileInfo.path().isBlank())
                .sorted(Comparator.comparing(this::modifiedAtOrMin).reversed()
                        .thenComparing(FileInfo::path, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public String readSessionLog(String userId, String logPath) {
        ReadResult result = filesystem.read(runtimeContext(userId), logPath, 0, 0);
        if (result == null || !result.isSuccess() || result.fileData() == null) {
            return null;
        }
        return result.fileData().content();
    }

    public String readSessionLogBySessionId(String userId, String sessionId) {
        String directPath = buildSessionLogPath(userId, sessionId);
        String content = readSessionLog(userId, directPath);
        if (content != null) {
            return content;
        }
        return findSessionLog(userId, sessionId)
                .map(FileInfo::path)
                .map(path -> readSessionLog(userId, path))
                .orElse(null);
    }

    public Optional<FileInfo> findSessionLog(String userId, String sessionId) {
        String expectedSuffix = "/" + sessionId + ".log.jsonl";
        return listSessionLogs(userId).stream()
                .filter(fileInfo -> fileInfo.path() != null)
                .filter(fileInfo -> fileInfo.path().endsWith(expectedSuffix))
                .findFirst();
    }

    public String buildSessionLogPath(String userId, String sessionId) {
        return "%s/%s/%s/%s.log.jsonl".formatted(
                AGENTS_ROOT,
                CoreConstants.Agent.BRAIN_AGENT_NAME,
                SESSION_DIR,
                sessionId);
    }

    public String buildSessionContextPath(String userId, String sessionId) {
        return "%s/%s/%s/%s.jsonl".formatted(
                AGENTS_ROOT,
                CoreConstants.Agent.BRAIN_AGENT_NAME,
                SESSION_DIR,
                sessionId);
    }

    public boolean deleteSessionFiles(String userId, String sessionId, String logPath) {
        boolean deleted = false;
        if (logPath != null && !logPath.isBlank()) {
            deleted = deleteFile(userId, logPath);
        } else {
            deleted = deleteFile(userId, buildSessionLogPath(userId, sessionId));
        }
        boolean contextDeleted = deleteFile(userId, buildSessionContextPath(userId, sessionId));
        return deleted || contextDeleted;
    }

    public boolean deleteFile(String userId, String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        try {
            return filesystem.delete(runtimeContext(userId), path).isSuccess();
        } catch (Exception ignored) {
            return false;
        }
    }

    private RuntimeContext runtimeContext(String userId) {
        return RuntimeContext.builder()
                .userId(userId)
                .build();
    }

    private Instant modifiedAtOrMin(FileInfo fileInfo) {
        try {
            if (fileInfo == null || fileInfo.modifiedAt() == null || fileInfo.modifiedAt().isBlank()) {
                return Instant.EPOCH;
            }
            return Instant.parse(fileInfo.modifiedAt());
        } catch (Exception ignored) {
            return Instant.EPOCH;
        }
    }
}
