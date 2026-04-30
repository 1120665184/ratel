package org.quyq.gwsu.common.database.metadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.database.enums.DatabaseType;
import org.quyq.gwsu.common.database.exception.DatabaseException;
import org.quyq.gwsu.common.database.metadata.dialect.MetadataDialect;
import org.quyq.gwsu.common.database.metadata.model.ColumnInfo;
import org.quyq.gwsu.common.database.metadata.model.DatabaseInfo;
import org.quyq.gwsu.common.database.metadata.model.ForeignKeyInfo;
import org.quyq.gwsu.common.database.metadata.model.TableInfo;
import org.quyq.gwsu.common.database.utils.DatabaseHelper;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据库元数据服务实现
 */
@Slf4j
@RequiredArgsConstructor
public class DdlFactory {

    private final DatabaseHelper databaseHelper;
    private final DataSource dataSource;
    private final Map<DatabaseType, MetadataDialect> dialectMap;

    public DdlFactory(DatabaseHelper databaseHelper, DataSource dataSource, List<MetadataDialect> dialects) {
        this.databaseHelper = databaseHelper;
        this.dataSource = dataSource;
        this.dialectMap = dialects != null
                ? dialects.stream().collect(Collectors.toMap(MetadataDialect::getSupportedType, Function.identity()))
                : Map.of();
    }


    /**
     * 获取数据库/Schema列表
     *
     * @return 数据库信息列表（MySQL返回数据库列表，PostgreSQL返回Schema列表）
     */
    public List<DatabaseInfo> showDatabases() {
        return executeWithDialect(MetadataDialect::showDatabases);
    }

    /**
     * 获取表列表
     *
     * @param databaseOrSchema 数据库名(MySQL) 或 Schema名(PostgreSQL)，为null时自动从当前连接获取
     * @return 表信息列表
     */
    public List<TableInfo> showTables(String databaseOrSchema) {
        return executeWithDialect((dialect, connection) -> dialect.showTables(connection, databaseOrSchema));
    }

    /**
     * 获取列列表
     *
     * @param databaseOrSchema 数据库名(MySQL) 或 Schema名(PostgreSQL)，为null时自动从当前连接获取
     * @param tableName 表名
     * @return 列信息列表
     */
    public List<ColumnInfo> showColumns(String databaseOrSchema, String tableName) {
        return executeWithDialect((dialect, connection) -> dialect.showColumns(connection, databaseOrSchema, tableName));
    }

    /**
     * 获取表的外键列表
     *
     * @param databaseOrSchema 数据库名(MySQL) 或 Schema名(PostgreSQL)，为null时自动从当前连接获取
     * @param tableName 表名
     * @return 外键信息列表
     */
    public List<ForeignKeyInfo> showForeignKeys(String databaseOrSchema, String tableName) {
        return executeWithDialect((dialect, connection) -> dialect.showForeignKeys(connection, databaseOrSchema, tableName));
    }

    /**
     * 使用方言执行操作
     *
     * @param action 要执行的操作
     * @return 操作结果
     */
    private <T> T executeWithDialect(DialectAction<T> action) {
        DatabaseType databaseType = databaseHelper.getCurrentDatabaseType();
        MetadataDialect dialect = dialectMap.get(databaseType);

        if (Objects.isNull(dialect)) {
            throw new DatabaseException("不支持的数据库方言：" + databaseType);
        }

        Connection connection = null;
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            return action.execute(dialect, connection);
        } catch (SQLException e) {
            log.error("获取数据库元数据失败", e);
            DatabaseException ex = new DatabaseException("获取数据库元数据失败: " + e.getMessage());
            ex.initCause(e);
            throw ex;
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

    /**
     * 方言操作函数式接口
     */
    @FunctionalInterface
    private interface DialectAction<T> {
        T execute(MetadataDialect dialect, Connection connection) throws SQLException;
    }
}
