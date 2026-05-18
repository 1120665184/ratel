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
import org.quyq.gwsu.common.security.dataresource.database.DBDataResourceConditionBuilder;
import org.quyq.gwsu.common.security.domain.DataPermissionInfo;
import org.quyq.gwsu.common.security.domain.DataResoureRule;
import org.quyq.gwsu.common.security.enums.DataResourceAssertType;
import org.quyq.gwsu.common.security.enums.DataScope;
import org.quyq.gwsu.common.security.exception.SecurityException;
import org.quyq.gwsu.common.security.utils.DataPermissionUtils;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    private final SecurityUtils securityUtils;
    private final DataResourceRuleUtils ruleUtils;
    private final DataPermissionUtils dataPermissionUtils;
    private final DataResourceConditionBuilder<Expression, Expression> conditionBuilder = new DBDataResourceConditionBuilder();

    /**
     * SELF_ONLY 模式下，用户资源字段固定为 username
     */
    private static final String SELF_ONLY_USER_RESOURCE_FIELD = "username";

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

        DataPermissionInfo userDataPermission = dataPermissionUtils.getUserDataPermission();
        // 为空或拥有全部数据权限时不添加条件过滤
        if (Objects.isNull(userDataPermission) || DataScope.ALL == userDataPermission.dataScope()) {
            return;
        }

        Map<String, List<?>> dataResource = userDataPermission.permissions();
        ;
        Map<String, List<DataResoureRule>> rulesByTable;

        if (DataScope.SELF_ONLY == userDataPermission.dataScope()) {
            // SELF_ONLY 模式：过滤出支持 SELF_ONLY 的规则，重构 conditions 复用已有逻辑
            String currentUsername = securityUtils.getUsername();
            if (currentUsername == null) {
                return;
            }

            List<DataResoureRule> allRules = ruleUtils.getAllRules();
            // 过滤出 supportSelfOnly=true 且 selfOnlyField 非空的规则，重构 conditions
            List<DataResoureRule> selfOnlyRules = allRules.stream()
                    .filter(rule -> Boolean.TRUE.equals(rule.getSupportSelfOnly())
                            && Objects.nonNull(rule.getSelfOnlyField())
                            && !rule.getSelfOnlyField().isBlank())
                    .map(this::buildSelfOnlyRule)
                    .toList();

            if (CollectionUtils.isEmpty(selfOnlyRules)) {
                return;
            }

            rulesByTable = selfOnlyRules.stream()
                    .collect(Collectors.groupingBy(DataResoureRule::getTableName));
        } else {
            rulesByTable = ruleUtils.getRulesGroupByTable();
        }

        if (rulesByTable.isEmpty()) {
            return;
        }

        String originalSql = boundSql.getSql();

        try {
            Statement statement = CCJSqlParserUtil.parse(originalSql);

            if (!(statement instanceof Select select)) {
                return;
            }

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
     * 构建 SELF_ONLY 模式的规则
     * 在原有 conditions 基础上，固定追加一个 FieldCondition：
     * - fieldName = selfOnlyField
     * - showNull = false
     * - assertType = EQ
     * - userResourceFields = ['username']
     *
     * @param original 原始规则
     * @return 追加了 SELF_ONLY 条件的规则副本
     */
    private DataResoureRule buildSelfOnlyRule(DataResoureRule original) {
        DataResoureRule rule = new DataResoureRule();
        rule.setDatabaseName(original.getDatabaseName());
        rule.setTableName(original.getTableName());
        rule.setSupportSelfOnly(original.getSupportSelfOnly());
        rule.setSelfOnlyField(original.getSelfOnlyField());

        // 复制原有 conditions
        List<DataResoureRule.FieldCondition> conditions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(original.getConditions())) {
            conditions.addAll(original.getConditions());
        }

        // 固定追加 SELF_ONLY 条件
        DataResoureRule.FieldCondition selfOnlyCondition = new DataResoureRule.FieldCondition();
        selfOnlyCondition.setFieldName(original.getSelfOnlyField());
        selfOnlyCondition.setShowNull(false);
        selfOnlyCondition.setAssertType(DataResourceAssertType.EQ);
        selfOnlyCondition.setUserResourceFields(List.of(SELF_ONLY_USER_RESOURCE_FIELD));
        conditions.add(selfOnlyCondition);

        rule.setConditions(conditions);
        return rule;
    }

    /**
     * 处理 SELECT 语句
     */
    private boolean processSelect(Select select, Map<String, List<DataResoureRule>> rulesByTable,
                                  Map<String, List<?>> dataResource) {
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

        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem != null) {
            modified = processFromItem(fromItem, plainSelect, null, rulesByTable, dataResource) || modified;
        }

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

        String alias = table.getAlias() != null ? table.getAlias().getName() : tableName;

        Expression condition = buildCondition(rules, alias, dataResource);

        if (condition == null) {
            return false;
        }

        if (join == null) {
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(condition);
            } else {
                plainSelect.setWhere(new AndExpression(where, condition));
            }
        } else {
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
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

}
