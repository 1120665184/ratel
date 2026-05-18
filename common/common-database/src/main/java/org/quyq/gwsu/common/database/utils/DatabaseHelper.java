package org.quyq.gwsu.common.database.utils;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.ds.ItemDataSource;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.database.enums.DatabaseType;
import org.quyq.gwsu.common.database.exception.DatabaseException;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class DatabaseHelper {

    private final DataSource dataSource;

    private final DynamicDataSourceProperties properties;

    /**
     * 获取所有数据源key
     * @return
     */
    public List<String> getAllDatasourceKeys() {
        return new ArrayList<>(properties.getDatasource().keySet());
    }

    /**
     * 获取当前数据源
     * @return
     */
    public String getCurrentDatasourceKey() {
        String currDataSource = DynamicDataSourceContextHolder.peek();
        if (StringUtils.isBlank(currDataSource))
            currDataSource = properties.getPrimary();
        return currDataSource;
    }

    /**
     * 获取当前数据源的库类型
     * @return
     */
    public DatabaseType getCurrentDatabaseType() {
        return getCurrentDatabaseType(dataSource);
    }

    /**
     * 通过数据源获取当前数据库类型
     *
     * @param dataSource
     * @return
     */
    public DatabaseType getCurrentDatabaseType(DataSource dataSource) {
        if (dataSource instanceof DynamicRoutingDataSource drDataSource) {
            return getCurrentDatabaseType(drDataSource.getDataSource(getCurrentDatasourceKey()));
        } else if (dataSource instanceof ItemDataSource itemDataSource) {
            return getCurrentDatabaseType(itemDataSource.getRealDataSource());
        } else if (isTargetDatasource(dataSource, "com.zaxxer.hikari.HikariDataSource")) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            return DatabaseType.fromDriverClassName(hikariDataSource.getDriverClassName());
        } else if (isTargetDatasource(dataSource, "com.alibaba.druid.pool.DruidDataSource")) {
            DruidDataSource druidDataSource = (DruidDataSource) dataSource;
            return DatabaseType.fromDriverClassName(druidDataSource.getDriverClassName());
        }

        try {
            Connection connection = null;
            try {
                connection = DataSourceUtils.getConnection(dataSource);
                DatabaseMetaData metaData = connection.getMetaData();
                String url = metaData.getURL();
                DatabaseType type = DatabaseType.fromJdbcUrl(url);
                if (type != null) {
                    return type;
                }
                String driverName = metaData.getDriverName();
                return DatabaseType.fromDriverClassName(driverName);
            } finally {
                if (Objects.nonNull(connection)) {
                    DataSourceUtils.releaseConnection(connection, dataSource);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException(CommonErrorCode.E01001, e);
        }

    }


    private boolean isTargetDatasource(DataSource dataSource, String targetSourceName) {
        try {
            Class<?> hikariClass = Class.forName(targetSourceName);
            return hikariClass.isInstance(dataSource);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }


}
