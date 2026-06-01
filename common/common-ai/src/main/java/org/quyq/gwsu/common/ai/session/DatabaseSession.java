package org.quyq.gwsu.common.ai.session;

import io.agentscope.core.session.ListHashUtil;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.state.SimpleSessionKey;
import io.agentscope.core.state.State;
import io.agentscope.core.util.JsonUtils;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 数据库持久化session（支持用户隔离）
 * <p>
 * 表结构需包含 user_id 字段：
 * CREATE TABLE [表名] (
 * session_id VARCHAR(255) NOT NULL,
 * state_key  VARCHAR(255) NOT NULL,
 * item_index INT          NOT NULL DEFAULT 0,
 * state_data TEXT         NOT NULL,
 * user_id    VARCHAR(24),                          -- 关联登录用户
 * created_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
 * updated_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
 * PRIMARY KEY (session_id, state_key, item_index)
 * );
 *
 * @param dataSource
 * @param tableName
 */
public record DatabaseSession(DataSource dataSource, String tableName) implements Session {
    private static final String DEFAULT_TABLE_NAME = "agentscope_sessions";
    private static final String HASH_KEY_SUFFIX = ":_hash";
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_-]*$");
    private static final int MAX_IDENTIFIER_LENGTH = 64;

    public DatabaseSession(DataSource dataSource) {
        this(dataSource, DEFAULT_TABLE_NAME);
    }

    public DatabaseSession(DataSource dataSource, String tableName) {
        if (dataSource == null) {
            throw new AgentException("DataSource cannot be null");
        }
        this.dataSource = dataSource;
        this.tableName = tableName != null && !tableName.trim().isEmpty() ? tableName.trim() : DEFAULT_TABLE_NAME;
        this.validateIdentifier(this.tableName, "Table name");
    }

    private String getFullTableName() {
        return this.tableName;
    }

    private String extractUserId(SessionKey sessionKey) {
        if (sessionKey instanceof CommonSessionKey commonKey) {
            return commonKey.userId();
        }
        return null;
    }

    // ==================== 单值保存 ====================
    @Override
    public void save(SessionKey sessionKey, String key, State value) {
        String sessionId = sessionKey.toIdentifier();
        String userId = extractUserId(sessionKey);
        this.validateSessionId(sessionId);
        this.validateStateKey(key);
        final int itemIndex = 0;

        String json = JsonUtils.getJsonCodec().toJson(value);

        try (Connection conn = this.dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 更新：userId为空时不加user_id条件；非空时加
                String updateSql = "UPDATE " + getFullTableName()
                        + " SET state_data = ? WHERE session_id = ? AND state_key = ? AND item_index = ?"
                        + (userId != null ? " AND user_id = ?" : "");
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    int paramIndex = 1;
                    stmt.setString(paramIndex++, json);
                    stmt.setString(paramIndex++, sessionId);
                    stmt.setString(paramIndex++, key);
                    stmt.setInt(paramIndex++, itemIndex);
                    if (userId != null) {
                        stmt.setString(paramIndex, userId);
                    }
                    int updated = stmt.executeUpdate();
                    if (updated == 0) {
                        String insertSql;
                        if (userId != null) {
                            insertSql = "INSERT INTO " + getFullTableName()
                                    + " (session_id, state_key, item_index, state_data, user_id) VALUES (?, ?, ?, ?, ?)";
                        } else {
                            insertSql = "INSERT INTO " + getFullTableName()
                                    + " (session_id, state_key, item_index, state_data) VALUES (?, ?, ?, ?)";
                        }
                        try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                            paramIndex = 1;
                            insStmt.setString(paramIndex++, sessionId);
                            insStmt.setString(paramIndex++, key);
                            insStmt.setInt(paramIndex++, itemIndex);
                            insStmt.setString(paramIndex++, json);
                            if (userId != null) {
                                insStmt.setString(paramIndex, userId);
                            }
                            insStmt.executeUpdate();
                        }
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话（sessionId:%s）存储失败 , key:%s 。".formatted(sessionId, key), e);
        }
    }

    // ==================== 列表保存（hash 也带上 user_id） ====================
    @Override
    public void save(SessionKey sessionKey, String key, List<? extends State> values) {
        String sessionId = sessionKey.toIdentifier();
        String userId = extractUserId(sessionKey);
        this.validateSessionId(sessionId);
        this.validateStateKey(key);
        if (values.isEmpty()) {
            return;
        }
        String hashKey = key + HASH_KEY_SUFFIX;

        try (Connection conn = this.dataSource.getConnection()) {
            String currentHash = ListHashUtil.computeHash(values);
            String storedHash = this.getStoredHash(conn, sessionId, hashKey, userId);
            int existingCount = this.getListCount(conn, sessionId, key, userId);
            boolean needsFullRewrite = ListHashUtil.needsFullRewrite(values, storedHash, existingCount);

            if (needsFullRewrite) {
                conn.setAutoCommit(false);
                try {
                    this.deleteListItems(conn, sessionId, key, userId);
                    this.insertAllItems(conn, sessionId, key, values, userId);
                    this.saveHash(conn, sessionId, hashKey, currentHash, userId);
                    conn.commit();
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } else if (values.size() > existingCount) {
                List<? extends State> newItems = values.subList(existingCount, values.size());
                this.insertItems(conn, sessionId, key, newItems, existingCount, userId);
                this.saveHash(conn, sessionId, hashKey, currentHash, userId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save list: " + key, e);
        }
    }

    // ---- Hash 操作（支持 user_id） ----
    private String getStoredHash(Connection conn, String sessionId, String hashKey, String userId) throws SQLException {
        String selectSql = "SELECT state_data FROM " + getFullTableName()
                + " WHERE session_id = ? AND state_key = ? AND item_index = ?"
                + (userId != null ? " AND user_id = ?" : "");
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            int paramIndex = 1;
            stmt.setString(paramIndex++, sessionId);
            stmt.setString(paramIndex++, hashKey);
            stmt.setInt(paramIndex++, 0);
            if (userId != null) {
                stmt.setString(paramIndex, userId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("state_data") : null;
            }
        }
    }

    private void saveHash(Connection conn, String sessionId, String hashKey, String hash, String userId) throws SQLException {
        // 更新
        String updateSql = "UPDATE " + getFullTableName()
                + " SET state_data = ? WHERE session_id = ? AND state_key = ? AND item_index = ?"
                + (userId != null ? " AND user_id = ?" : "");
        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            int paramIndex = 1;
            stmt.setString(paramIndex++, hash);
            stmt.setString(paramIndex++, sessionId);
            stmt.setString(paramIndex++, hashKey);
            stmt.setInt(paramIndex++, 0);
            if (userId != null) {
                stmt.setString(paramIndex, userId);
            }
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                String insertSql;
                if (userId != null) {
                    insertSql = "INSERT INTO " + getFullTableName()
                            + " (session_id, state_key, item_index, state_data, user_id) VALUES (?, ?, ?, ?, ?)";
                } else {
                    insertSql = "INSERT INTO " + getFullTableName()
                            + " (session_id, state_key, item_index, state_data) VALUES (?, ?, ?, ?)";
                }
                try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                    paramIndex = 1;
                    insStmt.setString(paramIndex++, sessionId);
                    insStmt.setString(paramIndex++, hashKey);
                    insStmt.setInt(paramIndex++, 0);
                    insStmt.setString(paramIndex++, hash);
                    if (userId != null) {
                        insStmt.setString(paramIndex, userId);
                    }
                    insStmt.executeUpdate();
                }
            }
        }
    }

    // ---- 列表项操作（已支持 user_id） ----
    private void deleteListItems(Connection conn, String sessionId, String key, String userId) throws SQLException {
        String deleteSql = "DELETE FROM " + getFullTableName() + " WHERE session_id = ? AND state_key = ?"
                + (userId != null ? " AND user_id = ?" : "");
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, sessionId);
            stmt.setString(2, key);
            if (userId != null) {
                stmt.setString(3, userId);
            }
            stmt.executeUpdate();
        }
    }

    private void insertAllItems(Connection conn, String sessionId, String key, List<? extends State> values, String userId) throws Exception {
        this.insertItems(conn, sessionId, key, values, 0, userId);
    }

    private void insertItems(Connection conn, String sessionId, String key, List<? extends State> items, int startIndex, String userId) throws Exception {
        String insertSql;
        if (userId != null) {
            insertSql = "INSERT INTO " + getFullTableName()
                    + " (session_id, state_key, item_index, state_data, user_id) VALUES (?, ?, ?, ?, ?)";
        } else {
            insertSql = "INSERT INTO " + getFullTableName()
                    + " (session_id, state_key, item_index, state_data) VALUES (?, ?, ?, ?)";
        }
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            int index = startIndex;
            for (State item : items) {
                String json = JsonUtils.getJsonCodec().toJson(item);
                int paramIndex = 1;
                stmt.setString(paramIndex++, sessionId);
                stmt.setString(paramIndex++, key);
                stmt.setInt(paramIndex++, index++);
                stmt.setString(paramIndex++, json);
                if (userId != null) {
                    stmt.setString(paramIndex, userId);
                }
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private int getListCount(Connection conn, String sessionId, String key, String userId) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM " + getFullTableName()
                + " WHERE session_id = ? AND state_key = ?"
                + (userId != null ? " AND user_id = ?" : "");
        try (PreparedStatement stmt = conn.prepareStatement(countSql)) {
            stmt.setString(1, sessionId);
            stmt.setString(2, key);
            if (userId != null) {
                stmt.setString(3, userId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ==================== 查询操作（userId 为空时不加 user_id 条件） ====================
    @Override
    public <T extends State> Optional<T> get(SessionKey sessionKey, String key, Class<T> type) {
        String sessionId = sessionKey.toIdentifier();
        String userId = extractUserId(sessionKey);
        this.validateSessionId(sessionId);
        this.validateStateKey(key);
        String selectSql = "SELECT state_data FROM " + getFullTableName()
                + " WHERE session_id = ? AND state_key = ? AND item_index = ?"
                + (userId != null ? " AND user_id = ?" : "");
        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            int paramIndex = 1;
            stmt.setString(paramIndex++, sessionId);
            stmt.setString(paramIndex++, key);
            stmt.setInt(paramIndex++, 0);
            if (userId != null) {
                stmt.setString(paramIndex, userId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("state_data");
                    return Optional.of(JsonUtils.getJsonCodec().fromJson(json, type));
                }
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get state: " + key, e);
        }
    }

    @Override
    public <T extends State> List<T> getList(SessionKey sessionKey, String key, Class<T> itemType) {
        String sessionId = sessionKey.toIdentifier();
        String userId = extractUserId(sessionKey);
        this.validateSessionId(sessionId);
        this.validateStateKey(key);
        String selectSql;
        if (userId != null) {
            selectSql = "SELECT state_data FROM " + getFullTableName()
                    + " WHERE session_id = ? AND state_key = ? AND user_id = ? ORDER BY item_index";
        } else {
            selectSql = "SELECT state_data FROM " + getFullTableName()
                    + " WHERE session_id = ? AND state_key = ? ORDER BY item_index";
        }
        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            int paramIndex = 1;
            stmt.setString(paramIndex++, sessionId);
            stmt.setString(paramIndex++, key);
            if (userId != null) {
                stmt.setString(paramIndex, userId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    String json = rs.getString("state_data");
                    result.add(JsonUtils.getJsonCodec().fromJson(json, itemType));
                }
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get list: " + key, e);
        }
    }

    @Override
    public boolean exists(SessionKey sessionKey) {
        String sessionId = sessionKey.toIdentifier();
        String userId = extractUserId(sessionKey);
        this.validateSessionId(sessionId);
        String existsSql;
        if (userId != null) {
            existsSql = "SELECT 1 FROM " + getFullTableName() + " WHERE session_id = ? AND user_id = ? LIMIT 1";
        } else {
            existsSql = "SELECT 1 FROM " + getFullTableName() + " WHERE session_id = ? LIMIT 1";
        }
        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(existsSql)) {
            stmt.setString(1, sessionId);
            if (userId != null) {
                stmt.setString(2, userId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check session existence: " + sessionId, e);
        }
    }

    @Override
    public void delete(SessionKey sessionKey) {
        String sessionId = sessionKey.toIdentifier();
        String userId = extractUserId(sessionKey);
        this.validateSessionId(sessionId);
        String deleteSql;
        if (userId != null) {
            deleteSql = "DELETE FROM " + getFullTableName() + " WHERE session_id = ? AND user_id = ?";
        } else {
            deleteSql = "DELETE FROM " + getFullTableName() + " WHERE session_id = ?";
        }
        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, sessionId);
            if (userId != null) {
                stmt.setString(2, userId);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new AgentException(CommonErrorCode.E05002,
                    "智能体会话（sessionId:%s）删除失败".formatted(sessionId), e);
        }
    }

    @Override
    public Set<SessionKey> listSessionKeys() {
        String listSql = "SELECT DISTINCT session_id FROM " + getFullTableName() + " ORDER BY session_id";
        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(listSql);
             ResultSet rs = stmt.executeQuery()) {
            Set<SessionKey> sessionKeys = new HashSet<>();
            while (rs.next()) {
                sessionKeys.add(SimpleSessionKey.of(rs.getString("session_id")));
            }
            return sessionKeys;
        } catch (SQLException e) {
            throw new AgentException(e);
        }
    }

    @Override
    public void close() {
        // Nothing to close
    }

    @Deprecated
    public int clearAllSessions() {
        String clearSql = "DELETE FROM " + getFullTableName();
        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(clearSql)) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new AgentException(e);
        }
    }

    public int truncateAllSessions() {
        String truncateSql = "TRUNCATE TABLE " + getFullTableName();
        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(truncateSql)) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new AgentException(e);
        }
    }

    // ==================== 验证方法 ====================
    protected void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new AgentException("Session ID cannot be null or empty");
        }
        if (sessionId.contains("/") || sessionId.contains("\\")) {
            throw new AgentException("Session ID cannot contain path separators");
        }
        if (sessionId.length() > 255) {
            throw new AgentException("Session ID cannot exceed 255 characters");
        }
    }

    private void validateStateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new AgentException("State key cannot be null or empty");
        }
        if (key.length() > 255) {
            throw new AgentException("State key cannot exceed 255 characters");
        }
    }

    private void validateIdentifier(String identifier, String identifierType) {
        if (identifier == null || identifier.isEmpty()) {
            throw new AgentException(identifierType + " cannot be null or empty");
        }
        if (identifier.length() > MAX_IDENTIFIER_LENGTH) {
            throw new AgentException(identifierType + " cannot exceed " + MAX_IDENTIFIER_LENGTH + " characters");
        }
        if (!IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new AgentException(identifierType + " contains invalid characters. Only alphanumeric characters, underscores, and hyphens are allowed, and it must start with a letter or underscore. Invalid value: " + identifier);
        }
    }
}