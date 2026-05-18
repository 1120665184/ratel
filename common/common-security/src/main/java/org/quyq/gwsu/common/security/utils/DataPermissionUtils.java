package org.quyq.gwsu.common.security.utils;


import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.dataresource.DataResourceConditionBuilder;
import org.quyq.gwsu.common.security.dataresource.DataResourceRuleUtils;
import org.quyq.gwsu.common.security.dataresource.database.DBDataResourceConditionBuilder;
import org.quyq.gwsu.common.security.domain.DataPermissionInfo;
import org.quyq.gwsu.common.security.domain.DataResoureRule;
import org.quyq.gwsu.common.security.enums.DataResourceAssertType;
import org.quyq.gwsu.common.security.enums.DataScope;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Quyq
 * @date 2026/5/8
 * @description 数据权限信息存储工具类
 */
@Slf4j
@RequiredArgsConstructor
public class DataPermissionUtils {

    private final DataResourceRuleUtils ruleUtils;

    private final SecurityUtils securityUtils;

    private final SessionUtils sessionUtils;

    private final ThreadLocal<DataPermissionInfo> dataPermissionInfos = new TransmittableThreadLocal<>();

    private final DataResourceConditionBuilder<Expression, Expression> conditionBuilder = new DBDataResourceConditionBuilder();


    /**
     * 获取当前用户的数据权限信息
     *
     * @return
     */
    public DataPermissionInfo getUserDataPermission() {
        DataPermissionInfo info = dataPermissionInfos.get();
        if (Objects.nonNull(info)) {
            return info;
        }

        return securityUtils.getSubject()
                .map(subject -> {

                    Optional<Map<String, List<?>>> dataResource = sessionUtils.getValue(SecurityConstants.Session.SESSION_CURR_DATA_RESOURCE);
                    return new DataPermissionInfo(subject.getDataScope(), dataResource.orElse(Collections.emptyMap()));
                }).orElse(null);

    }


    /**
     * SELF_ONLY 模式下，用户资源字段固定为 username
     */
    private static final String SELF_ONLY_USER_RESOURCE_FIELD = "username";

    /**
     * 对SQL追加数据权限过滤条件
     *
     * @param sql 原始SQL语句
     * @return 追加权限过滤条件后的SQL，如果无需追加则返回原始SQL
     */
    public String applyDataPermission(String sql) {
        DataPermissionInfo userDataPermission = getUserDataPermission();

        // 为空或拥有全部数据权限时不添加条件过滤
        if (Objects.isNull(userDataPermission) || DataScope.ALL == userDataPermission.dataScope()) {
            return sql;
        }

        return applyDataPermission(sql, userDataPermission);
    }

    /**
     * 对SQL追加数据权限过滤条件（使用指定的数据权限信息）
     *
     * @param sql                原始SQL语句
     * @param dataPermissionInfo 数据权限信息
     * @return 追加权限过滤条件后的SQL，如果无需追加则返回原始SQL
     */
    public String applyDataPermission(String sql, DataPermissionInfo dataPermissionInfo) {
        if (dataPermissionInfo == null || DataScope.ALL == dataPermissionInfo.dataScope()) {
            return sql;
        }

        Map<String, List<?>> dataResource = dataPermissionInfo.permissions();
        Map<String, List<DataResoureRule>> rulesByTable;

        if (DataScope.SELF_ONLY == dataPermissionInfo.dataScope()) {
            String currentUsername = securityUtils.getUsername();
            if (currentUsername == null) {
                return sql;
            }

            List<DataResoureRule> allRules = ruleUtils.getAllRules();
            List<DataResoureRule> selfOnlyRules = allRules.stream()
                    .filter(rule -> Boolean.TRUE.equals(rule.getSupportSelfOnly())
                            && Objects.nonNull(rule.getSelfOnlyField())
                            && !rule.getSelfOnlyField().isBlank())
                    .map(this::buildSelfOnlyRule)
                    .toList();

            if (CollectionUtils.isEmpty(selfOnlyRules)) {
                return sql;
            }

            rulesByTable = selfOnlyRules.stream()
                    .collect(Collectors.groupingBy(DataResoureRule::getTableName));
        } else {
            rulesByTable = ruleUtils.getRulesGroupByTable();
        }

        if (rulesByTable.isEmpty()) {
            return sql;
        }

        try {
            Statement statement = CCJSqlParserUtil.parse(sql);

            if (!(statement instanceof Select select)) {
                return sql;
            }

            boolean modified = processSelect(select, rulesByTable, dataResource);

            if (modified) {
                String newSql = select.toString();
                log.debug("Data resource SQL modified: {} -> {}", sql, newSql);
                return newSql;
            }

        } catch (JSQLParserException e) {
            log.warn("Failed to parse SQL for data resource filtering: {}", sql, e);
        }

        return sql;
    }

    /**
     * 构建 SELF_ONLY 模式的规则
     */
    private DataResoureRule buildSelfOnlyRule(DataResoureRule original) {
        DataResoureRule rule = new DataResoureRule();
        rule.setDatabaseName(original.getDatabaseName());
        rule.setTableName(original.getTableName());
        rule.setSupportSelfOnly(original.getSupportSelfOnly());
        rule.setSelfOnlyField(original.getSelfOnlyField());

        List<DataResoureRule.FieldCondition> conditions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(original.getConditions())) {
            conditions.addAll(original.getConditions());
        }

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
     * 设置当前用户的数据权限缓存
     *
     * @param info
     */
    public void setDataPermission(DataPermissionInfo info) {
        dataPermissionInfos.set(info);
    }


    /**
     * 清空当前用户的数据权限缓存
     */
    public void clearDataPermission() {
        dataPermissionInfos.remove();
    }
}
