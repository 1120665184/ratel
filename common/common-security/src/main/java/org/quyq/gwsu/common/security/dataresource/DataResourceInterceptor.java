package org.quyq.gwsu.common.security.dataresource;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.dataresource.database.DBDataResourceConditionBuilder;
import org.quyq.gwsu.common.security.domain.DataResoureRule;
import org.quyq.gwsu.common.security.exception.SecurityException;
import org.quyq.gwsu.common.security.utils.SessionUtils;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * 数据权限拦截器
 * 基于 MyBatis Plus InnerInterceptor 实现，拦截 SELECT 语句并拼接数据权限过滤条件
 *
 * @author Quyq
 * @date 2026/4/20
 */
@Slf4j
@RequiredArgsConstructor
public class DataResourceInterceptor implements InnerInterceptor, Ordered {

    private final SessionUtils sessionUtils;
    private final DataResourceRuleUtils ruleUtils;
    private final DataResourceConditionBuilder<Expression, Expression> conditionBuilder = new DBDataResourceConditionBuilder();

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

        // 获取当前用户数据资源
        @SuppressWarnings("unchecked")
        Map<String, List<?>> dataResource = (Map<String, List<?>>) sessionUtils.getValue(
                SecurityConstants.Session.SESSION_CURR_DATA_RESOURCE
        ).orElse(null);

        if (dataResource == null || dataResource.isEmpty()) {
            return;
        }


        // 按表名分组
        Map<String, List<DataResoureRule>> rulesByTable = ruleUtils.getRulesGroupByTable();
        if (rulesByTable.isEmpty()) {
            return;
        }

        String originalSql = boundSql.getSql();

        try {
            Statement statement = CCJSqlParserUtil.parse(originalSql);

            if (!(statement instanceof Select select)) {
                return;
            }

            // 处理 SELECT 语句
            boolean modified = processSelect(select, rulesByTable, dataResource);

            if (modified) {
                String newSql = select.toString();
                setSql(boundSql, newSql);
                log.debug("Data resource SQL modified: {} -> {}", originalSql, newSql);
            }

        } catch (JSQLParserException e) {
            log.warn("Failed to parse SQL for data resource filtering: {}", originalSql, e);
        }
    }

    /**
     * 处理 SELECT 语句
     *
     * @param select       SELECT 语句
     * @param rulesByTable 按表名分组的规则
     * @param dataResource 用户数据资源
     * @return 是否有修改
     */
    private boolean processSelect(Select select, Map<String, List<DataResoureRule>> rulesByTable,
                                  Map<String, List<?>> dataResource) {
        // 在 JSqlParser 5.2 中，Select 本身就是抽象类
        // PlainSelect、SetOperationList、Values 都是 Select 的子类
        return processSelectStatement(select, rulesByTable, dataResource);
    }

    /**
     * 处理 Select 语句（PlainSelect、SetOperationList、Values）
     */
    private boolean processSelectStatement(Select select, Map<String, List<DataResoureRule>> rulesByTable,
                                           Map<String, List<?>> dataResource) {
        if (select instanceof PlainSelect plainSelect) {
            return processPlainSelect(plainSelect, rulesByTable, dataResource);
        } else if (select instanceof SetOperationList setOp) {
            boolean modified = false;
            for (Select body : setOp.getSelects()) {
                modified = processSelectStatement(body, rulesByTable, dataResource) || modified;
            }
            return modified;
        } else if (select instanceof Values) {
            // VALUES 语句不需要处理
            return false;
        }
        return false;
    }

    /**
     * 处理 PlainSelect
     */
    private boolean processPlainSelect(PlainSelect plainSelect, Map<String, List<DataResoureRule>> rulesByTable,
                                       Map<String, List<?>> dataResource) {
        boolean modified = false;

        // 处理主表
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem != null) {
            modified = processFromItem(fromItem, plainSelect, null, rulesByTable, dataResource) || modified;
        }

        // 处理 JOIN 表
        List<Join> joins = plainSelect.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                FromItem rightItem = join.getRightItem();
                if (rightItem != null) {
                    modified = processFromItem(rightItem, plainSelect, join, rulesByTable, dataResource) || modified;
                }
            }
        }

        return modified;
    }

    /**
     * 处理 FromItem（表、子查询等）
     */
    private boolean processFromItem(FromItem fromItem, PlainSelect plainSelect, Join join,
                                    Map<String, List<DataResoureRule>> rulesByTable,
                                    Map<String, List<?>> dataResource) {

        if (fromItem instanceof Table table) {
            return processTable(table, plainSelect, join, rulesByTable, dataResource);
        } else if (fromItem instanceof ParenthesedSelect parenthesedSelect) {
            // 处理括号包裹的子查询（包括普通子查询）
            Select subSelectBody = parenthesedSelect.getSelect();
            if (subSelectBody != null) {
                return processSelectStatement(subSelectBody, rulesByTable, dataResource);
            }
        }

        return false;
    }

    /**
     * 处理表，应用数据权限过滤
     */
    private boolean processTable(Table table, PlainSelect plainSelect, Join join,
                                 Map<String, List<DataResoureRule>> rulesByTable,
                                 Map<String, List<?>> dataResource) {

        String tableName = table.getName();
        List<DataResoureRule> rules = rulesByTable.get(tableName);

        if (CollectionUtils.isEmpty(rules)) {
            return false;
        }

        // 获取表别名
        String alias = table.getAlias() != null ? table.getAlias().getName() : tableName;

        // 构建过滤条件
        Expression condition = buildCondition(rules, alias, dataResource);

        if (condition == null) {
            return false;
        }

        if (join == null) {
            // 主表：添加到 WHERE 子句
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(condition);
            } else {
                plainSelect.setWhere(new AndExpression(where, condition));
            }
        } else {
            // JOIN 表：添加到 ON 子句
            Expression onExpression = join.getOnExpression();
            if (onExpression == null) {
                join.setOnExpression(condition);
            } else {
                join.setOnExpression(new AndExpression(onExpression, condition));
            }
        }

        return true;
    }

    /**
     * 构建过滤条件表达式
     */
    private Expression buildCondition(List<DataResoureRule> rules, String alias,
                                      Map<String, List<?>> dataResource) {

        Expression result = null;

        for (DataResoureRule rule : rules) {
            Expression condition = conditionBuilder.build(rule, alias, dataResource);
            if (condition != null) {
                if (result == null) {
                    result = condition;
                } else {
                    result = new AndExpression(result, condition);
                }
            }
        }

        return result;
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
        // 在分页拦截器之前执行
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

}
