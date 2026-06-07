package org.quyq.gwsu.common.security.utils;


import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.quyq.gwsu.common.database.metadata.DdlFactory;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.dataresource.DataResourceConditionBuilder;
import org.quyq.gwsu.common.security.dataresource.DataResourceRuleUtils;
import org.quyq.gwsu.common.security.dataresource.database.DBDataResourceConditionBuilder;
import org.quyq.gwsu.common.security.domain.DataPermissionInfo;
import org.quyq.gwsu.common.security.domain.DataResoureRule;
import org.quyq.gwsu.common.security.enums.DataScope;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Quyq
 * @date 2026/5/8
 * @description 数据权限信息存储工具类
 */
@Slf4j
public class DataPermissionUtils {

    private final DataResourceRuleUtils ruleUtils;

    private final SecurityUtils securityUtils;

    private final SessionUtils sessionUtils;

    private final DdlFactory ddlFactory;

    private final ThreadLocal<DataPermissionInfo> dataPermissionInfos = new ThreadLocal<>();

    private final DataResourceConditionBuilder<Expression, Expression> conditionBuilder = new DBDataResourceConditionBuilder();

    /**
     * SELF_ONLY 模式下，用户资源字段固定为 username
     */
    private static final String SELF_ONLY_USER_RESOURCE_FIELD = "username";

    public DataPermissionUtils(DataResourceRuleUtils ruleUtils, SecurityUtils securityUtils,
                               SessionUtils sessionUtils, DdlFactory ddlFactory) {
        this.ruleUtils = ruleUtils;
        this.securityUtils = securityUtils;
        this.sessionUtils = sessionUtils;
        this.ddlFactory = ddlFactory;
    }

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
        Map<String, List<DataResoureRule>> rulesByTable = resolveRules(dataPermissionInfo);

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
     * 根据数据权限范围解析规则
     */
    private Map<String, List<DataResoureRule>> resolveRules(DataPermissionInfo dataPermissionInfo) {
        if (DataScope.SELF_ONLY == dataPermissionInfo.dataScope()) {
            String currentUsername = securityUtils.getUsername();
            if (currentUsername == null) {
                return Collections.emptyMap();
            }

            List<DataResoureRule> allRules = ruleUtils.getAllRules();
            List<DataResoureRule> selfOnlyRules = allRules.stream()
                    .filter(rule -> Boolean.TRUE.equals(rule.getSupportSelfOnly())
                            && Objects.nonNull(rule.getSelfOnlyField())
                            && !rule.getSelfOnlyField().isBlank())
                    .map(this::buildSelfOnlyRule)
                    .toList();

            if (CollectionUtils.isEmpty(selfOnlyRules)) {
                return Collections.emptyMap();
            }

            return selfOnlyRules.stream()
                    .collect(Collectors.groupingBy(DataResoureRule::getTableName));
        } else {
            return ruleUtils.getRulesGroupByTable();
        }
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
        selfOnlyCondition.setAssertType(org.quyq.gwsu.common.security.enums.DataResourceAssertType.EQ);
        selfOnlyCondition.setUserResourceFields(List.of(SELF_ONLY_USER_RESOURCE_FIELD));
        conditions.add(selfOnlyCondition);

        rule.setConditions(conditions);
        return rule;
    }

    // ==================== SQL 结构遍历 ====================

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
     * 处理 PlainSelect：遍历 FROM、JOIN、WHERE、HAVING、SELECT 列中所有可能包含的表和子查询
     */
    private boolean processPlainSelect(PlainSelect plainSelect, Map<String, List<DataResoureRule>> rulesByTable,
                                       Map<String, List<?>> dataResource) {
        boolean modified = false;

        // 1. 处理 FROM
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem != null) {
            modified = processFromItem(fromItem, plainSelect, null, rulesByTable, dataResource) || modified;
        }

        // 2. 处理 JOIN
        List<Join> joins = plainSelect.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                FromItem rightItem = join.getRightItem();
                if (rightItem != null) {
                    modified = processFromItem(rightItem, plainSelect, join, rulesByTable, dataResource) || modified;
                }
                // 处理 JOIN ON 条件中的子查询
                Collection<Expression> onExpressions = join.getOnExpressions();
                if (onExpressions != null) {
                    for (Expression onEx : onExpressions) {
                        modified = processExpression(onEx, rulesByTable, dataResource) || modified;
                    }
                }
            }
        }

        // 3. 处理 WHERE 子句中的子查询
        if (plainSelect.getWhere() != null) {
            modified = processExpression(plainSelect.getWhere(), rulesByTable, dataResource) || modified;
        }

        // 4. 处理 HAVING 子句中的子查询
        if (plainSelect.getHaving() != null) {
            modified = processExpression(plainSelect.getHaving(), rulesByTable, dataResource) || modified;
        }

        // 5. 处理 SELECT 列中的子查询
        List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
        if (selectItems != null) {
            for (SelectItem<?> item : selectItems) {
                if (item.getExpression() != null) {
                    modified = processExpression(item.getExpression(), rulesByTable, dataResource) || modified;
                }
            }
        }

        return modified;
    }

    /**
     * 处理 FromItem（表、子查询、ParenthesedFromItem、TableFunction）
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
        } else if (fromItem instanceof ParenthesedFromItem parenthesedFromItem) {
            // 处理括号包裹的 FROM 项，如 (t1 JOIN t2 ON ...)
            boolean modified = false;
            FromItem innerFromItem = parenthesedFromItem.getFromItem();
            if (innerFromItem != null) {
                modified = processFromItem(innerFromItem, plainSelect, join, rulesByTable, dataResource) || modified;
            }
            List<Join> innerJoins = parenthesedFromItem.getJoins();
            if (innerJoins != null) {
                for (Join innerJoin : innerJoins) {
                    FromItem rightItem = innerJoin.getRightItem();
                    if (rightItem != null) {
                        modified = processFromItem(rightItem, plainSelect, innerJoin, rulesByTable, dataResource) || modified;
                    }
                }
            }
            return modified;
        } else if (fromItem instanceof TableFunction tableFunction) {
            // 处理表函数，如 generate_series()，遍历其参数中可能的子查询
            Function function = tableFunction.getFunction();
            if (function != null && function.getParameters() != null) {
                boolean modified = false;
                for (Expression param : function.getParameters()) {
                    modified = processExpression(param, rulesByTable, dataResource) || modified;
                }
                return modified;
            }
        }
        return false;
    }

    // ==================== 表匹配与条件追加 ====================

    /**
     * 处理表，应用数据权限过滤
     */
    private boolean processTable(Table table, PlainSelect plainSelect, Join join,
                                 Map<String, List<DataResoureRule>> rulesByTable,
                                 Map<String, List<?>> dataResource) {
        String tableName = table.getName();
        List<DataResoureRule> rules = findRulesForTable(table, rulesByTable);

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
     * 根据表名匹配规则，再用库名过滤
     * <p>
     * 匹配逻辑：
     * 1. 按纯表名从分组中获取规则列表
     * 2. 从 SQL 表引用或 DdlFactory 获取库名/模式名
     * 3. 用库名对规则列表做过滤：
     * - 规则未配置 databaseName → 匹配（适用于所有库的同名表）
     * - 规则配置了 databaseName 且与当前库名一致 → 匹配
     * - 规则配置了 databaseName 但与当前库名不一致 → 不匹配
     */
    private List<DataResoureRule> findRulesForTable(Table table, Map<String, List<DataResoureRule>> rulesByTable) {
        String tableName = table.getName();
        List<DataResoureRule> rules = rulesByTable.get(tableName);

        if (CollectionUtils.isEmpty(rules)) {
            return rules;
        }

        // 只有一条规则且未配置库名，无需过滤
        if (rules.size() == 1 && rules.getFirst().getDatabaseName() == null) {
            return rules;
        }

        // 所有规则都未配置库名，无需过滤
        if (rules.stream().allMatch(rule -> rule.getDatabaseName() == null)) {
            return rules;
        }

        // 获取当前表所属的库名/模式名
        String databaseName = resolveDatabaseName(table);

        // 用库名过滤规则列表
        return rules.stream()
                .filter(rule -> rule.getDatabaseName() == null
                        || databaseName == null
                        || databaseName.equalsIgnoreCase(rule.getDatabaseName()))
                .toList();
    }

    /**
     * 解析表所属的库名/模式名
     * <p>
     * 优先从 SQL 表引用中提取（如 db.table），否则从 DdlFactory 获取当前数据源的库名
     */
    private String resolveDatabaseName(Table table) {
        // 从全限定名中提取库名：如 catalog.schema.table 或 schema.table
        String fullName = table.getFullyQualifiedName();
        String tableName = table.getName();

        if (fullName != null && !fullName.equals(tableName)) {
            String[] parts = fullName.split("\\.");
            if (parts.length == 2) {
                // schema.table 形式，第一段是库名/模式名
                return parts[0].toLowerCase();
            } else if (parts.length >= 3) {
                // catalog.schema.table 形式，第二段是 scheme
                return parts[1].toLowerCase();
            }
        }

        // SQL 中无库名前缀，从当前数据源获取
        try {
            return ddlFactory.getCurrentDatabaseOrSchema();
        } catch (Exception e) {
            log.debug("Failed to get current database name from DdlFactory", e);
            return null;
        }
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

    // ==================== 表达式递归遍历 ====================

    /**
     * 递归处理表达式，发现其中嵌套的子查询并追加权限条件
     * <p>
     * 覆盖以下场景：
     * - ParenthesedSelect（子查询）
     * - Function（函数参数中的子查询）
     * - CaseExpression（CASE WHEN 中的子查询）
     * - BinaryExpression（二元表达式中的子查询）
     * - 其他 Expression 子类（通过反射兜底遍历）
     */
    private boolean processExpression(Expression expression, Map<String, List<DataResoureRule>> rulesByTable,
                                      Map<String, List<?>> dataResource) {
        if (expression == null) {
            return false;
        }

        // 子查询
        if (expression instanceof ParenthesedSelect parenthesedSelect) {
            return processSelect(parenthesedSelect, rulesByTable, dataResource);
        }

        // 函数
        if (expression instanceof Function function) {
            boolean modified = false;
            if (function.getParameters() != null) {
                for (Expression param : function.getParameters()) {
                    modified = processExpression(param, rulesByTable, dataResource) || modified;
                }
            }
            return modified;
        }

        // CASE 表达式
        if (expression instanceof CaseExpression caseExpression) {
            boolean modified = false;

            if (caseExpression.getSwitchExpression() != null) {
                modified = processExpression(caseExpression.getSwitchExpression(), rulesByTable, dataResource) || modified;
            }

            List<WhenClause> whenClauses = caseExpression.getWhenClauses();
            if (whenClauses != null) {
                for (WhenClause whenClause : whenClauses) {
                    if (whenClause.getWhenExpression() != null) {
                        modified = processExpression(whenClause.getWhenExpression(), rulesByTable, dataResource) || modified;
                    }
                    if (whenClause.getThenExpression() != null) {
                        modified = processExpression(whenClause.getThenExpression(), rulesByTable, dataResource) || modified;
                    }
                }
            }

            if (caseExpression.getElseExpression() != null) {
                modified = processExpression(caseExpression.getElseExpression(), rulesByTable, dataResource) || modified;
            }

            return modified;
        }

        // 二元表达式（AndExpression, OrExpression, EqualsTo, LikeExpression 等）
        if (expression instanceof BinaryExpression binaryExpression) {
            boolean modified = false;
            if (binaryExpression.getLeftExpression() != null) {
                modified = processExpression(binaryExpression.getLeftExpression(), rulesByTable, dataResource) || modified;
            }
            if (binaryExpression.getRightExpression() != null) {
                modified = processExpression(binaryExpression.getRightExpression(), rulesByTable, dataResource) || modified;
            }
            return modified;
        }

        // 反射兜底：遍历其他 Expression 子类中类型为 Expression 的字段
        return processExpressionByReflection(expression, rulesByTable, dataResource);
    }

    /**
     * 通过反射兜底遍历 Expression 子类中类型为 Expression 的字段
     * <p>
     * 用于处理 JSQLParser 中尚未显式编码的 Expression 子类
     * （如 Between、InExpression、NotExpression、ExistsExpression 等），
     * 确保它们内部嵌套的子查询不会被遗漏。
     * <p>
     * 注意：此方法涉及反射，需要在 AOT 编译时通过 RuntimeHintsRegistrar 注册相关类的反射元数据。
     */
    private boolean processExpressionByReflection(Expression expression, Map<String, List<DataResoureRule>> rulesByTable,
                                                  Map<String, List<?>> dataResource) {
        // 跳过已知已处理的类型和不需要递归的简单类型
        Class<?> clazz = expression.getClass();
        if (clazz.getName().startsWith("net.sf.jsqlparser.expression.") &&
                isSimpleExpressionType(clazz)) {
            return false;
        }

        boolean modified = false;
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            Class<?> fieldType = field.getType();

            // 字段类型是 Expression 或其子接口/子类
            if (Expression.class.isAssignableFrom(fieldType)) {
                try {
                    field.setAccessible(true);
                    Expression fieldValue = (Expression) field.get(expression);
                    if (fieldValue != null) {
                        modified = processExpression(fieldValue, rulesByTable, dataResource) || modified;
                    }
                } catch (IllegalAccessException e) {
                    log.debug("Failed to access field {} on {}", field.getName(), clazz.getSimpleName(), e);
                }
            }
        }

        return modified;
    }

    /**
     * 判断是否为简单的、不需要递归遍历的 Expression 类型
     * 这些类型不可能包含子查询或表引用
     */
    private boolean isSimpleExpressionType(Class<?> clazz) {
        return clazz == net.sf.jsqlparser.expression.StringValue.class
                || clazz == net.sf.jsqlparser.expression.LongValue.class
                || clazz == net.sf.jsqlparser.expression.DoubleValue.class
                || clazz == net.sf.jsqlparser.expression.DateValue.class
                || clazz == net.sf.jsqlparser.expression.TimeValue.class
                || clazz == net.sf.jsqlparser.expression.TimestampValue.class
                || clazz == net.sf.jsqlparser.expression.NullValue.class
                || clazz == net.sf.jsqlparser.schema.Column.class
                || clazz == net.sf.jsqlparser.expression.SignedExpression.class
                || clazz == net.sf.jsqlparser.expression.UserVariable.class
                || clazz == net.sf.jsqlparser.expression.HexValue.class
                || clazz == net.sf.jsqlparser.expression.BooleanValue.class;
    }

    // ==================== 权限缓存 ====================

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
