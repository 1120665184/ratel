package org.quyq.gwsu.common.database.provider;


import lombok.RequiredArgsConstructor;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.database.enums.DatabaseType;
import org.quyq.gwsu.common.database.exception.DatabaseException;
import org.quyq.gwsu.common.database.utils.DatabaseHelper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * @author Quyq
 * @date 2026/3/19
 * @description
 */
@RequiredArgsConstructor
public class DynamicDatabaseIdProvider implements DatabaseIdProvider {

    private final DatabaseHelper databaseHelper;

    private Properties properties;


    @Override
    public String getDatabaseId(DataSource dataSource) {
        if (dataSource == null) {
            throw new DatabaseException("数据源不能为空");
        }
        try {
            return getDatabaseName(dataSource);
        } catch (SQLException e) {
            throw new DatabaseException(CommonErrorCode.E01001, e);
        }
    }

    @Override
    public void setProperties(Properties p) {
        this.properties = p;
    }

    private String getDatabaseName(DataSource dataSource) throws SQLException {

        DatabaseType databaseType = databaseHelper.getCurrentDatabaseType(dataSource);
        if (Objects.nonNull(databaseType)) {
            return databaseType.getCode();
        }

        String productName = getDatabaseProductName(dataSource);
        if (properties == null || properties.isEmpty()) {
            return productName;
        }
        return properties.entrySet().stream().filter(entry -> productName.contains((String) entry.getKey()))
                .map(entry -> (String) entry.getValue()).findFirst().orElse(null);
    }

    private String getDatabaseProductName(DataSource dataSource) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            return con.getMetaData().getDatabaseProductName();
        }
    }
}
