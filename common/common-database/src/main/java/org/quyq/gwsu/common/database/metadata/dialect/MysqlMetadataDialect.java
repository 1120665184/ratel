package org.quyq.gwsu.common.database.metadata.dialect;

import org.quyq.gwsu.common.database.enums.DatabaseType;
import org.quyq.gwsu.common.database.metadata.model.DatabaseInfo;
import org.quyq.gwsu.common.database.utils.SqlExecutor;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL 元数据方言
 */
public class MysqlMetadataDialect extends AbstractMetadataDialect {

    @Override
    public List<DatabaseInfo> showDatabases(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        List<DatabaseInfo> databases = new ArrayList<>();

        try (ResultSet rs = metaData.getCatalogs()) {
            while (rs.next()) {
                String catalogName = rs.getString("TABLE_CAT");
                DatabaseInfo databaseInfo = DatabaseInfo.builder()
                        .name(catalogName)
                        .remark(null)
                        .build();
                databases.add(databaseInfo);
            }
        }
        return databases;
    }

    @Override
    public DatabaseType getSupportedType() {
        return DatabaseType.MYSQL;
    }

    @Override
    protected String getCatalog(String databaseOrSchema) {
        // MySQL 使用 catalog（数据库名）
        return databaseOrSchema;
    }

    @Override
    protected String getSchema(String databaseOrSchema) {
        // MySQL 不使用 schema 参数
        return null;
    }

    @Override
    protected String doGetCurrentDatabaseOrSchema(Connection connection) throws SQLException {
        // MySQL 使用 catalog 表示数据库
        return connection.getCatalog();
    }

    /**
     * MySQL 特有：获取数据库详细信息（包含字符集和排序规则）
     *
     * @param connection 数据库连接
     * @param databaseName 数据库名
     * @return 数据库信息
     */
    public DatabaseInfo getDatabaseDetail(Connection connection, String databaseName) throws SQLException {
        String sql = "SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME " +
                "FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '" + databaseName + "'";

        String[][] result = SqlExecutor.executeSqlAndReturnArr(connection, sql);
        if (result.length > 0) {
            String[] row = result[0];
            return DatabaseInfo.builder()
                    .name(row[0])
                    .charset(row[1])
                    .collation(row[2])
                    .build();
        }
        return null;
    }
}
