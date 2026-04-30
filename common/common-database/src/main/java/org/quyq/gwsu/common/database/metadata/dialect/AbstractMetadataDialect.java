package org.quyq.gwsu.common.database.metadata.dialect;

import org.quyq.gwsu.common.database.metadata.model.ColumnInfo;
import org.quyq.gwsu.common.database.metadata.model.ForeignKeyInfo;
import org.quyq.gwsu.common.database.metadata.model.TableInfo;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 抽象元数据方言基类
 * 提供基于 JDBC DatabaseMetaData 的通用实现
 */
public abstract class AbstractMetadataDialect implements MetadataDialect {

    @Override
    public List<TableInfo> showTables(Connection connection, String databaseOrSchema) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        List<TableInfo> tables = new ArrayList<>();

        String effectiveDatabaseOrSchema = resolveDatabaseOrSchema(connection, databaseOrSchema);

        try (ResultSet rs = metaData.getTables(
                getCatalog(effectiveDatabaseOrSchema),
                getSchema(effectiveDatabaseOrSchema),
                "%",
                new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                TableInfo tableInfo = TableInfo.builder()
                        .name(rs.getString("TABLE_NAME"))
                        .remark(rs.getString("REMARKS"))
                        .type(rs.getString("TABLE_TYPE"))
                        .schema(rs.getString("TABLE_SCHEM"))
                        .catalog(rs.getString("TABLE_CAT"))
                        .build();
                tables.add(tableInfo);
            }
        }
        return tables;
    }

    @Override
    public List<ColumnInfo> showColumns(Connection connection, String databaseOrSchema, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        List<ColumnInfo> columns = new ArrayList<>();

        String effectiveDatabaseOrSchema = resolveDatabaseOrSchema(connection, databaseOrSchema);

        // 获取主键集合
        Set<String> primaryKeys = getPrimaryKeys(metaData, effectiveDatabaseOrSchema, tableName);

        try (ResultSet rs = metaData.getColumns(
                getCatalog(effectiveDatabaseOrSchema),
                getSchema(effectiveDatabaseOrSchema),
                tableName,
                "%")) {
            while (rs.next()) {
                ColumnInfo columnInfo = ColumnInfo.builder()
                        .name(rs.getString("COLUMN_NAME"))
                        .type(rs.getString("TYPE_NAME"))
                        .nullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")))
                        .defaultValue(rs.getString("COLUMN_DEF"))
                        .remark(rs.getString("REMARKS"))
                        .position(rs.getInt("ORDINAL_POSITION"))
                        .length(rs.getInt("COLUMN_SIZE"))
                        .precision(rs.getInt("COLUMN_SIZE"))
                        .scale(rs.getInt("DECIMAL_DIGITS"))
                        .isPrimaryKey(primaryKeys.contains(rs.getString("COLUMN_NAME")))
                        .build();
                columns.add(columnInfo);
            }
        }
        return columns;
    }

    @Override
    public List<ForeignKeyInfo> showForeignKeys(Connection connection, String databaseOrSchema, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        List<ForeignKeyInfo> foreignKeys = new ArrayList<>();

        String effectiveDatabaseOrSchema = resolveDatabaseOrSchema(connection, databaseOrSchema);

        try (ResultSet rs = metaData.getImportedKeys(
                getCatalog(effectiveDatabaseOrSchema),
                getSchema(effectiveDatabaseOrSchema),
                tableName)) {
            while (rs.next()) {
                ForeignKeyInfo foreignKeyInfo = ForeignKeyInfo.builder()
                        .name(rs.getString("FK_NAME"))
                        .columnName(rs.getString("FKCOLUMN_NAME"))
                        .referencedTableName(rs.getString("PKTABLE_NAME"))
                        .referencedColumnName(rs.getString("PKCOLUMN_NAME"))
                        .tableName(rs.getString("FKTABLE_NAME"))
                        .updateRule(rs.getInt("UPDATE_RULE"))
                        .deleteRule(rs.getInt("DELETE_RULE"))
                        .keySeq(rs.getInt("KEY_SEQ"))
                        .build();
                foreignKeys.add(foreignKeyInfo);
            }
        }
        return foreignKeys;
    }

    /**
     * 解析数据库/Schema名称，若参数为null则从连接中获取当前值
     *
     * @param connection 数据库连接
     * @param databaseOrSchema 数据库名或Schema名，可为null
     * @return 有效的数据库/Schema名称
     * @throws SQLException SQL执行异常
     */
    protected String resolveDatabaseOrSchema(Connection connection, String databaseOrSchema) throws SQLException {
        if (databaseOrSchema != null) {
            return databaseOrSchema;
        }
        return getCurrentDatabaseOrSchema(connection);
    }

    /**
     * 从连接中获取当前的数据库或Schema名称
     *
     * @param connection 数据库连接
     * @return 当前的数据库/Schema名称
     * @throws SQLException SQL执行异常
     */
    protected abstract String getCurrentDatabaseOrSchema(Connection connection) throws SQLException;

    /**
     * 获取表的主键列名集合
     */
    protected Set<String> getPrimaryKeys(DatabaseMetaData metaData, String databaseOrSchema, String tableName) throws SQLException {
        Set<String> primaryKeys = new HashSet<>();
        try (ResultSet rs = metaData.getPrimaryKeys(
                getCatalog(databaseOrSchema),
                getSchema(databaseOrSchema),
                tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }
        return primaryKeys;
    }

    /**
     * 获取 Catalog 参数
     * 不同数据库对 catalog 和 schema 的处理不同
     *
     * @param databaseOrSchema 数据库名或 Schema 名
     * @return Catalog 参数值
     */
    protected abstract String getCatalog(String databaseOrSchema);

    /**
     * 获取 Schema 参数
     * 不同数据库对 catalog 和 schema 的处理不同
     *
     * @param databaseOrSchema 数据库名或 Schema 名
     * @return Schema 参数值
     */
    protected abstract String getSchema(String databaseOrSchema);
}
