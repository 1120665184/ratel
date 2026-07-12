package org.quyq.gwsu.common.ai.session;

import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;
import io.agentscope.core.util.JsonUtils;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 数据库持久化 AgentStateStore，沿用历史会话表结构并增加 userId 维度隔离。
 */
public record DatabaseStateStore(DataSource dataSource, String tableName) implements AgentStateStore {

    private static final String DEFAULT_TABLE_NAME = "security_brain_sessions";
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_-]*$");
    private static final int MAX_IDENTIFIER_LENGTH = 64;

    public DatabaseStateStore(DataSource dataSource) {
        this(dataSource, DEFAULT_TABLE_NAME);
    }

    public DatabaseStateStore(DataSource dataSource, String tableName) {
        if (dataSource == null) {
            throw new AgentException("DataSource cannot be null");
        }
        this.dataSource = dataSource;
        this.tableName = tableName != null && !tableName.trim().isEmpty() ? tableName.trim() : DEFAULT_TABLE_NAME;
        validateIdentifier(this.tableName, "Table name");
    }

    @Override
    public void save(String userId, String sessionId, String stateKey, State value) {
        validateSessionId(sessionId);
        validateStateKey(stateKey);
        String json = JsonUtils.getJsonCodec().toJson(value);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int updated = executeUpdate(conn, """
                        UPDATE %s
                           SET state_data = ?
                         WHERE session_id = ?
                           AND state_key = ?
                           AND item_index = 0
                           AND user_id = ?
                        """.formatted(tableName), json, sessionId, stateKey, normalizeUserId(userId));

                if (updated == 0) {
                    executeUpdate(conn, """
                            INSERT INTO %s (session_id, state_key, item_index, state_data, user_id)
                            VALUES (?, ?, 0, ?, ?)
                            """.formatted(tableName), sessionId, stateKey, json, normalizeUserId(userId));
                }
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception ex) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话存储失败，sessionId=%s, stateKey=%s".formatted(sessionId, stateKey), ex);
        }
    }

    @Override
    public void save(String userId, String sessionId, String stateKey, List<? extends State> values) {
        validateSessionId(sessionId);
        validateStateKey(stateKey);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                executeUpdate(conn, """
                        DELETE FROM %s
                         WHERE session_id = ?
                           AND state_key = ?
                           AND user_id = ?
                        """.formatted(tableName), sessionId, stateKey, normalizeUserId(userId));

                if (!values.isEmpty()) {
                    String insertSql = """
                            INSERT INTO %s (session_id, state_key, item_index, state_data, user_id)
                            VALUES (?, ?, ?, ?, ?)
                            """.formatted(tableName);
                    try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                        int index = 0;
                        for (State item : values) {
                            stmt.setString(1, sessionId);
                            stmt.setString(2, stateKey);
                            stmt.setInt(3, index++);
                            stmt.setString(4, JsonUtils.getJsonCodec().toJson(item));
                            stmt.setString(5, normalizeUserId(userId));
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception ex) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话列表存储失败，sessionId=%s, stateKey=%s".formatted(sessionId, stateKey), ex);
        }
    }

    @Override
    public <T extends State> Optional<T> get(String userId, String sessionId, String stateKey, Class<T> type) {
        validateSessionId(sessionId);
        validateStateKey(stateKey);

        String sql = """
                SELECT state_data
                  FROM %s
                 WHERE session_id = ?
                   AND state_key = ?
                   AND item_index = 0
                   AND user_id = ?
                """.formatted(tableName);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionId);
            stmt.setString(2, stateKey);
            stmt.setString(3, normalizeUserId(userId));
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(JsonUtils.getJsonCodec().fromJson(rs.getString("state_data"), type));
            }
        } catch (Exception ex) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话读取失败，sessionId=%s, stateKey=%s".formatted(sessionId, stateKey), ex);
        }
    }

    @Override
    public <T extends State> List<T> getList(String userId, String sessionId, String stateKey, Class<T> itemType) {
        validateSessionId(sessionId);
        validateStateKey(stateKey);

        String sql = """
                SELECT state_data
                  FROM %s
                 WHERE session_id = ?
                   AND state_key = ?
                   AND user_id = ?
                 ORDER BY item_index ASC
                """.formatted(tableName);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionId);
            stmt.setString(2, stateKey);
            stmt.setString(3, normalizeUserId(userId));
            try (ResultSet rs = stmt.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(JsonUtils.getJsonCodec().fromJson(rs.getString("state_data"), itemType));
                }
                return result;
            }
        } catch (Exception ex) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话列表读取失败，sessionId=%s, stateKey=%s".formatted(sessionId, stateKey), ex);
        }
    }

    @Override
    public boolean exists(String userId, String sessionId) {
        validateSessionId(sessionId);

        String sql = """
                SELECT 1
                  FROM %s
                 WHERE session_id = ?
                   AND user_id = ?
                 LIMIT 1
                """.formatted(tableName);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionId);
            stmt.setString(2, normalizeUserId(userId));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ex) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话存在性查询失败，sessionId=%s".formatted(sessionId), ex);
        }
    }

    @Override
    public void delete(String userId, String sessionId) {
        validateSessionId(sessionId);
        try (Connection conn = dataSource.getConnection()) {
            executeUpdate(conn, """
                    DELETE FROM %s
                     WHERE session_id = ?
                       AND user_id = ?
                    """.formatted(tableName), sessionId, normalizeUserId(userId));
        } catch (Exception ex) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话删除失败，sessionId=%s".formatted(sessionId), ex);
        }
    }

    @Override
    public void delete(String userId, String sessionId, String stateKey) {
        validateSessionId(sessionId);
        validateStateKey(stateKey);
        try (Connection conn = dataSource.getConnection()) {
            executeUpdate(conn, """
                    DELETE FROM %s
                     WHERE session_id = ?
                       AND state_key = ?
                       AND user_id = ?
                    """.formatted(tableName), sessionId, stateKey, normalizeUserId(userId));
        } catch (Exception ex) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话状态删除失败，sessionId=%s, stateKey=%s".formatted(sessionId, stateKey), ex);
        }
    }

    @Override
    public Set<String> listSessionIds(String userId) {
        String sql = """
                SELECT DISTINCT session_id
                  FROM %s
                 WHERE user_id = ?
                """.formatted(tableName);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizeUserId(userId));
            try (ResultSet rs = stmt.executeQuery()) {
                Set<String> result = new HashSet<>();
                while (rs.next()) {
                    result.add(rs.getString("session_id"));
                }
                return result;
            }
        } catch (Exception ex) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话列表查询失败", ex);
        }
    }

    private int executeUpdate(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        }
    }

    private String normalizeUserId(String userId) {
        return Objects.toString(userId, "");
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new AgentException("Session ID cannot be blank");
        }
    }

    private void validateStateKey(String stateKey) {
        if (stateKey == null || stateKey.isBlank()) {
            throw new AgentException("State key cannot be blank");
        }
    }

    private void validateIdentifier(String identifier, String label) {
        if (identifier == null || identifier.isBlank()) {
            throw new AgentException(label + " cannot be blank");
        }
        if (identifier.length() > MAX_IDENTIFIER_LENGTH || !IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new AgentException(label + " contains illegal characters: " + identifier);
        }
    }
}
