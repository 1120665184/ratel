package org.quyq.gwsu.common.database.config;

import org.quyq.gwsu.common.database.metadata.DdlFactory;
import org.quyq.gwsu.common.database.metadata.dialect.MetadataDialect;
import org.quyq.gwsu.common.database.metadata.dialect.MysqlMetadataDialect;
import org.quyq.gwsu.common.database.metadata.dialect.PostgresqlMetadataDialect;
import org.quyq.gwsu.common.database.utils.DatabaseHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.util.List;

/**
 * 数据库元数据自动配置
 */
@AutoConfiguration
@ConditionalOnClass(DataSource.class)
public class DatabaseMetadataConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DdlFactory defaultDdlFactory(
            DatabaseHelper databaseHelper,
            DataSource dataSource,
            ObjectProvider<List<MetadataDialect>> dialectsProvider) {
        List<MetadataDialect> dialects = dialectsProvider.getIfAvailable();
        return new DdlFactory(databaseHelper, dataSource, dialects);
    }

    @Bean
    @ConditionalOnMissingBean
    public PostgresqlMetadataDialect postgresqlMetadataDialect() {
        return new PostgresqlMetadataDialect();
    }

    @Bean
    @ConditionalOnMissingBean
    public MysqlMetadataDialect mysqlMetadataDialect() {
        return new MysqlMetadataDialect();
    }
}
