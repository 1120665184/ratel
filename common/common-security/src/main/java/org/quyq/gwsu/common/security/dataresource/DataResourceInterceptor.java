package org.quyq.gwsu.common.security.dataresource;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.quyq.gwsu.common.security.exception.SecurityException;
import org.quyq.gwsu.common.security.utils.DataPermissionUtils;
import org.springframework.core.Ordered;

import java.lang.reflect.Field;

/**
 * 数据权限拦截器
 * 基于 MyBatis Plus InnerInterceptor 实现，拦截 SELECT 语句并通过 DataPermissionUtils 追加数据权限过滤条件
 *
 * @author Quyq
 * @date 2026/4/20
 */
@Slf4j
@RequiredArgsConstructor
public class DataResourceInterceptor implements InnerInterceptor, Ordered {

    private final DataPermissionUtils dataPermissionUtils;

    /**
     * BoundSql.sql 字段引用
     */
    private static final Field SQL_FIELD;

    static {
        try {
            SQL_FIELD = BoundSql.class.getDeclaredField("sql");
            SQL_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new SecurityException(e);
        }
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {

        // 只处理 SELECT 语句
        if (ms.getSqlCommandType() != SqlCommandType.SELECT) {
            return;
        }

        String originalSql = boundSql.getSql();
        String newSql = dataPermissionUtils.applyDataPermission(originalSql);

        if (newSql != originalSql) {
            setSql(boundSql, newSql);
            log.debug("Data resource SQL modified: {} -> {}", originalSql, newSql);
        }
    }

    /**
     * 设置 BoundSql 的 SQL 语句
     */
    private void setSql(BoundSql boundSql, String sql) {
        try {
            SQL_FIELD.set(boundSql, sql);
        } catch (IllegalAccessException e) {
            log.error("Failed to set SQL to BoundSql", e);
            throw new RuntimeException("Failed to set SQL to BoundSql", e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

}
