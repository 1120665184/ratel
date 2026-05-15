package org.quyq.gwsu.common.database.config;

import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.database.enums.DatabaseType;
import org.quyq.gwsu.common.database.provider.DefaultIdentifierGenerator;
import org.quyq.gwsu.common.database.provider.DynamicDatabaseIdProvider;
import org.quyq.gwsu.common.database.utils.DatabaseHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.Ordered;

import javax.sql.DataSource;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

@AutoConfiguration
@AutoConfigureAfter({DataSourceAutoConfiguration.class})
@MapperScan(CoreConstants.Project.COMMON_PACKAGE + ".**.mapper")
@ImportRuntimeHints({DBRuntimeHintsRegistrar.class})
public class DatabaseConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<List<InnerInterceptor>> interceptors) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        Optional.ofNullable(interceptors.getIfAvailable())
                .ifPresent(s ->
                        s.stream()
                                .sorted(Comparator.comparingInt(item ->
                                        item instanceof Ordered ordered ? ordered.getOrder() : Ordered.LOWEST_PRECEDENCE))
                                .forEach(interceptor::addInnerInterceptor)
                );


        return interceptor;
    }

    /**
     * 分页插件
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        return new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
    }


    @Bean
    public DatabaseHelper databaseHelper(DataSource dataSource, DynamicDataSourceProperties properties) {
        return new DatabaseHelper(dataSource, properties);
    }

    @Bean
    public DatabaseIdProvider databaseIdProvider(DatabaseHelper databaseHelper) {
        DynamicDatabaseIdProvider provider = new DynamicDatabaseIdProvider(databaseHelper);

        Properties p = new Properties();
        Stream.of(DatabaseType.values()).forEach(v -> p.put(v.getName(), v.getCode()));
        provider.setProperties(p);

        return provider;
    }

    /**
     * ID生成策略
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public IdentifierGenerator defaultIdentifierGenerator() {
        return new DefaultIdentifierGenerator();
    }


}
