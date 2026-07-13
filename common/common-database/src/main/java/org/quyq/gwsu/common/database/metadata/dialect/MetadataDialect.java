package org.quyq.gwsu.common.database.metadata.dialect;

import org.quyq.gwsu.common.database.enums.DatabaseType;
import org.quyq.gwsu.common.database.metadata.model.ColumnInfo;
import org.quyq.gwsu.common.database.metadata.model.DatabaseInfo;
import org.quyq.gwsu.common.database.metadata.model.ForeignKeyInfo;
import org.quyq.gwsu.common.database.metadata.model.TableInfo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据库元数据方言接口
 */
public interface MetadataDialect {

    /**
     * 获取数据库/Schema列表
     *
     * @param connection 数据库连接
     * @return 数据库/Schema信息列表
     * @throws SQLException SQL执行异常
     */
    List<DatabaseInfo> showDatabases(Connection connection) throws SQLException;

    /**
     * 获取表列表
     *
     * @param connection 数据库连接
     * @param databaseOrSchema 数据库名(MySQL) 或 Schema名(PostgreSQL)
     * @return 表信息列表
     * @throws SQLException SQL执行异常
     */
    List<TableInfo> showTables(Connection connection, String databaseOrSchema) throws SQLException;

    /**
     * 获取列列表
     *
     * @param connection 数据库连接
     * @param databaseOrSchema 数据库名(MySQL) 或 Schema名(PostgreSQL)
     * @param tableName 表名
     * @return 列信息列表
     * @throws SQLException SQL执行异常
     */
    List<ColumnInfo> showColumns(Connection connection, String databaseOrSchema, String tableName) throws SQLException;

    /**
     * 获取外键列表
     *
     * @param connection 数据库连接
     * @param databaseOrSchema 数据库名(MySQL) 或 Schema名(PostgreSQL)
     * @param tableName 表名
     * @return 外键信息列表
     * @throws SQLException SQL执行异常
     */
    List<ForeignKeyInfo> showForeignKeys(Connection connection, String databaseOrSchema, String tableName) throws SQLException;

    /**
     * 获取支持的数据库类型
     *
     * @return 数据库类型
     */
    DatabaseType getSupportedType();

    /**
     * 从连接中获取当前 Catalog 名称
     *
     * @param connection 数据库连接
     * @return 当前 Catalog 名称
     * @throws SQLException SQL执行异常
     */
    String getCurrentCatalog(Connection connection) throws SQLException;

    /**
     * 从连接中获取当前数据库/Schema 名称
     * MySQL 返回 catalog（库名），PostgreSQL 返回 schema（模式名）
     *
     * @param connection 数据库连接
     * @return 当前数据库/Schema 名称
     * @throws SQLException SQL执行异常
     */
    String getCurrentDatabaseSchema(Connection connection) throws SQLException;
}
