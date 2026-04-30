package org.quyq.gwsu.common.database.metadata.dialect;

import org.quyq.gwsu.common.database.enums.DatabaseType;
import org.quyq.gwsu.common.database.metadata.model.DatabaseInfo;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * PostgreSQL 元数据方言
 */
public class PostgresqlMetadataDialect extends AbstractMetadataDialect {

    /**
     * 系统 Schema 前缀，这些 Schema 会被排除
     */
    private static final Set<String> SYSTEM_SCHEMAS = Set.of(
            "pg_catalog",
            "information_schema",
            "pg_toast"
    );

    @Override
    public List<DatabaseInfo> showDatabases(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        List<DatabaseInfo> schemas = new ArrayList<>();

        try (ResultSet rs = metaData.getSchemas()) {
            while (rs.next()) {
                String schemaName = rs.getString("TABLE_SCHEM");
                // 排除系统 Schema
                if (isSystemSchema(schemaName)) {
                    continue;
                }
                DatabaseInfo databaseInfo = DatabaseInfo.builder()
                        .name(schemaName)
                        .remark(null)
                        .build();
                schemas.add(databaseInfo);
            }
        }
        return schemas;
    }

    @Override
    public DatabaseType getSupportedType() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    protected String getCatalog(String databaseOrSchema) {
        // PostgreSQL 使用 schema 而非 catalog
        return null;
    }

    @Override
    protected String getSchema(String databaseOrSchema) {
        return databaseOrSchema;
    }

    @Override
    protected String getCurrentDatabaseOrSchema(Connection connection) throws SQLException {
        // PostgreSQL 使用 schema
        return connection.getSchema();
    }

    /**
     * 判断是否为系统 Schema
     */
    private boolean isSystemSchema(String schemaName) {
        if (schemaName == null) {
            return false;
        }
        String lowerName = schemaName.toLowerCase();
        return SYSTEM_SCHEMAS.contains(lowerName) || lowerName.startsWith("pg_");
    }
}
