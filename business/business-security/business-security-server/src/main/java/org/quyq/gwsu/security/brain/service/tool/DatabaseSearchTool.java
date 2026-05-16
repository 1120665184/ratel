package org.quyq.gwsu.security.brain.service.tool;

import cn.hutool.core.collection.CollUtil;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.lang3.StringUtils;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.common.security.domain.vo.SqlQueryVO;
import org.quyq.gwsu.common.security.service.ISQLExecutionService;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelColumnVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelDetailVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelForeignKeyVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelTableVO;
import org.quyq.gwsu.security.brain.service.agent.DatabaseSearchAgent;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelTableService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * 数据库查询工具集
 * 提供给自然语言转SQL智能体使用的工具，包含获取表详情和执行SQL两个工具
 *
 * @author Quyq
 * @date 2026/5/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSearchTool {

    private final ISecurityTableModelTableService tableModelTableService;

    private final RestClient.Builder restClientBuilder;


    /**
     * 获取指定表的详细内容（字段信息），包含当前登录用户对该字段的权限
     *
     * @param tableName  表名
     * @param dataSource 数据源
     * @return 表详细信息（含字段权限）
     */
    @Tool(name = "GetTableDetail", description = "获取指定表的详细结构信息，包括字段名、类型、注释及当前用户是否拥有该字段的查询权限。在生成SQL之前必须先调用此工具了解表结构。")
    public String getTableDetail(
            @ToolParam(name = "modelPrefix", description = "所属模块/服务") String modelPrefix,
            @ToolParam(name = "tableName", description = "表名") String tableName,
            @ToolParam(name = "dataSource", description = "数据源名称，默认为master") String dataSource,
            DatabaseSearchAgent databaseSearchAgent) {

        if (dataSource == null || dataSource.isBlank()) {
            dataSource = "master";
        }


        // 获取表信息
        TableModelDetailVO tableDetail = tableModelTableService.getTableDetail(modelPrefix, dataSource, tableName);
        if (tableDetail == null) {
            return "未找到表 [" + tableName + "]（所属服务：" + modelPrefix + " ,数据源: " + dataSource + "），请检查表名和数据源是否正确。";
        }

        TableModelTableVO tableVO = tableDetail.getTable();
        // 获取字段列表
        List<TableModelColumnVO> columns = tableDetail.getColumns();

        // 获取外键列表
        List<TableModelForeignKeyVO> foreignKeys = tableDetail.getForeignKeys();

        // 获取当前用户的字段权限
        Map<String, FieldPermission> fieldPermissions = getCurrentUserFieldPermissions(databaseSearchAgent,
                modelPrefix, dataSource, tableName);

        // 构建结果
        StringBuilder sb = new StringBuilder();
        sb.append("## 表: ").append(tableName).append("（数据源: ").append(dataSource).append("）\n");
        sb.append("表注释: ").append(tableVO.getTableComment() != null ? tableVO.getTableComment() : "无").append("\n");
        sb.append("所属服务: ").append(tableVO.getModulePrefix()).append("\n\n");

        sb.append("### 字段列表\n");
        sb.append("| 字段名 | 类型 | 长度 | 可空 | 主键 | 注释 | 用户是否有权限 |\n");
        sb.append("|--------|------|------|------|------|------|------------|\n");

        for (TableModelColumnVO column : columns) {
            FieldPermission perm = fieldPermissions.get(column.getColumnName());
            boolean hasPermission = perm == null || perm.show();

            sb.append("| ").append(column.getColumnName())
                    .append(" | ").append(column.getColumnType() != null ? column.getColumnType() : "-")
                    .append(" | ").append(column.getColumnLength() != null ? column.getColumnLength() : "-")
                    .append(" | ").append(column.getIsNullable() != null && column.getIsNullable() ? "是" : "否")
                    .append(" | ").append(column.getIsPrimaryKey() != null && column.getIsPrimaryKey() ? "是" : "否")
                    .append(" | ").append(column.getColumnComment() != null ? column.getColumnComment() : "-")
                    .append(" | ").append(hasPermission ? "是" : "否")
                    .append(" |\n");
        }

        // 外键信息
        if (!CollectionUtils.isEmpty(foreignKeys)) {
            sb.append("\n### 外键约束\n");
            for (TableModelForeignKeyVO fk : foreignKeys) {
                sb.append("- 约束名: ").append(fk.getConstraintName() != null ? fk.getConstraintName() : "-")
                        .append("\n");
            }
        }

        // 权限提示
        List<String> deniedFields = columns.stream()
                .map(TableModelColumnVO::getColumnName)
                .filter(fieldName -> {
                    FieldPermission perm = fieldPermissions.get(fieldName);
                    return perm != null && !perm.show();
                })
                .toList();

        if (!deniedFields.isEmpty()) {
            sb.append("\n> **权限提示**: 以下字段当前用户无查询权限: ").append(String.join(", ", deniedFields))
                    .append("。生成SQL时请勿包含这些字段。\n");
        }

        return sb.toString();
    }

    /**
     * 执行SQL查询
     * 入参：所属服务、数据源、SQL语句
     * 步骤：1. 校验SQL中的表和字段当前用户是否都有权限  2. 追加权限过滤条件  3. 执行SQL
     *
     * @param modulePrefix 所属服务（模块前缀）
     * @param dataSource   数据源名称
     * @param sql          SQL语句（仅限SELECT）
     */
    @Tool(name = "ExecuteSql", description = "执行只读SQL查询（仅限SELECT语句）。会自动校验当前用户对SQL中涉及的表和字段的权限，并自动追加数据权限过滤条件。不同服务/数据源的表无法在同一条SQL中关联查询，请确保SQL中所有表属于同一服务同一数据源。")
    public String executeSql(
            @ToolParam(name = "modulePrefix", description = "所属服务（模块前缀），如security") String modulePrefix,
            @ToolParam(name = "dataSource", description = "数据源名称，默认为master") String dataSource,
            @ToolParam(name = "sql", description = "要执行的SELECT SQL语句") String sql,
            DatabaseSearchAgent databaseSearchAgent) {

        if (dataSource == null || dataSource.isBlank()) {
            dataSource = "master";
        }

        Map<String, Map<String, FieldPermission>> userTableModelPermission = databaseSearchAgent.getUserTableModelPermission();

        // 1. 校验SQL是否为SELECT语句
        String trimmedSql = sql.trim();
        if (!trimmedSql.toUpperCase().startsWith("SELECT")) {
            return "错误：仅允许执行SELECT查询语句，禁止执行任何修改操作（INSERT/UPDATE/DELETE等）。";
        }

        // 2. 校验SQL中涉及的表和字段当前用户是否都有权限（同时解析出 表名->列名 映射供脱敏使用）
        SqlParseResult parseResult = parseSqlMeta(userTableModelPermission, modulePrefix, dataSource, sql);
        if (parseResult.error() != null) {
            return "权限校验失败: " + parseResult.error();
        }

        // 3. 执行SQL
        SqlQueryVO result = doExecuteSql(modulePrefix, dataSource, sql);
        if (Objects.isNull(result)) {
            return "执行失败";
        }
        List<Map<String, Object>> queryResult = result.data();
        String finSql = result.executionSql();

        List<String> desensitizedFields = Collections.emptyList();

        StringBuilder dataStr = new StringBuilder("查询无数据");
        if (CollUtil.isNotEmpty(queryResult)) {
            // 4. 根据字段脱敏配置对结果进行脱敏处理（基于表名->列名映射精确定位字段所属表）
            desensitizedFields = applyDesensitization(queryResult, userTableModelPermission, modulePrefix, dataSource, parseResult.tableColumnMap());

            dataStr = new StringBuilder();
            // 表头
            List<String> headers = new ArrayList<>(queryResult.getFirst().keySet());
            dataStr.append("| ").append(String.join(" | ", headers)).append(" |\n");
            dataStr.append("| ").append(Collections.nCopies(headers.size(), "---").stream().reduce((a, b) -> a + " | " + b).orElse("")).append(" |\n");
            // 数据行
            for (Map<String, Object> row : queryResult) {
                dataStr.append("| ");
                for (String header : headers) {
                    Object val = row.get(header);
                    dataStr.append(val != null ? val.toString() : "NULL").append(" | ");
                }
                dataStr.append("\n");
            }
        }

        return """
                ## SQL执行结果
                
                **执行的SQL**:
                ```sql
                %s
                ```
                
                **执行结果**:
                %s
                
                %s
                """.formatted(finSql, dataStr.toString(),
                desensitizedFields.isEmpty() ? "" : "> **脱敏提示**: 以下字段因权限配置已做脱敏处理: " + String.join(", ", desensitizedFields)
        );

    }

    /**
     * 解析SQL元数据并校验权限
     * <p>
     * 校验逻辑：
     * 1. 解析SQL提取所有表名和别名映射
     * 2. 校验每个表是否属于指定的服务+数据源分组
     * 3. 校验每个表当前用户是否有表级权限（至少一个字段可见）
     * 4. 解析SQL提取所有列引用，校验字段级权限
     * 5. 构建 表名->列名集合 映射供脱敏使用
     *
     * @return 解析结果，包含错误信息和表名->列名映射
     */
    private SqlParseResult parseSqlMeta(Map<String, Map<String, FieldPermission>> userTableModelPermission,
                                        String modulePrefix, String dataSource, String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select select)) {
                return new SqlParseResult("仅支持SELECT语句的权限校验", Map.of());
            }

            // 1. 提取SQL中的表名与别名映射
            Map<String, String> aliasToTable = new LinkedHashMap<>();
            Set<String> tableNames = new LinkedHashSet<>();
            collectTableInfo(select, aliasToTable, tableNames);

            if (tableNames.isEmpty()) {
                return new SqlParseResult(null, Map.of());
            }

            // 2. 校验每个表：存在性 + 归属分组 + 表级权限
            for (String tableName : tableNames) {

                String key = modulePrefix + ":" + dataSource + ":" + tableName;
                Map<String, FieldPermission> fieldPerms = userTableModelPermission.get(key);
                if (Objects.isNull(fieldPerms)) {
                    return new SqlParseResult("当前用户无权访问表 [" + tableName + "]", Map.of());
                }
            }

            // 3. 校验字段级权限 + 构建 表名->列名集合 映射
            Set<Column> columns = new LinkedHashSet<>();
            collectColumns(select, columns);

            Map<String, Set<String>> tableColumnMap = new LinkedHashMap<>();
            List<String> deniedFields = new ArrayList<>();

            for (Column col : columns) {
                String colName = col.getColumnName();
                String tableAlias = col.getTable() != null ? col.getTable().getName() : null;
                String resolvedTableName = resolveTableName(tableAlias, aliasToTable, tableNames);
                if (resolvedTableName == null) {
                    continue;
                }

                // 构建表名->列名映射
                tableColumnMap.computeIfAbsent(resolvedTableName, k -> new LinkedHashSet<>()).add(colName);

                // 校验字段权限
                String key = modulePrefix + ":" + dataSource + ":" + resolvedTableName;
                Map<String, FieldPermission> fieldPerms = userTableModelPermission.get(key);
                if (fieldPerms != null && fieldPerms.containsKey(colName)) {
                    FieldPermission perm = fieldPerms.get(colName);
                    if (!perm.show()) {
                        deniedFields.add(resolvedTableName + "." + colName);
                    }
                }
            }

            if (!deniedFields.isEmpty()) {
                return new SqlParseResult("当前用户无权访问以下字段: " + String.join(", ", deniedFields), Map.of());
            }

            return new SqlParseResult(null, tableColumnMap);
        } catch (JSQLParserException e) {
            log.warn("SQL解析失败，无法校验权限: {}", sql, e);
            return new SqlParseResult("SQL解析失败，无法校验权限", Map.of());
        }
    }

    /**
     * SQL解析结果
     *
     * @param error          错误信息，null表示校验通过
     * @param tableColumnMap 表名 -> 列名集合 的映射
     */
    private record SqlParseResult(String error, Map<String, Set<String>> tableColumnMap) {
    }

    // ==================== SQL解析辅助方法 ====================

    /**
     * 从SELECT语句中收集表名和别名映射
     *
     * @param select       SELECT语句
     * @param aliasToTable 别名 -> 真实表名 的映射
     * @param tableNames   收集到的所有真实表名
     */
    private void collectTableInfo(Select select, Map<String, String> aliasToTable, Set<String> tableNames) {
        if (select instanceof PlainSelect plainSelect) {
            collectTableInfoFromPlainSelect(plainSelect, aliasToTable, tableNames);
        } else if (select instanceof SetOperationList setOp) {
            for (Select body : setOp.getSelects()) {
                collectTableInfo(body, aliasToTable, tableNames);
            }
        }
    }

    private void collectTableInfoFromPlainSelect(PlainSelect plainSelect, Map<String, String> aliasToTable, Set<String> tableNames) {
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem != null) {
            collectTableInfoFromItem(fromItem, aliasToTable, tableNames);
        }
        List<Join> joins = plainSelect.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                FromItem rightItem = join.getRightItem();
                if (rightItem != null) {
                    collectTableInfoFromItem(rightItem, aliasToTable, tableNames);
                }
            }
        }
    }

    private void collectTableInfoFromItem(FromItem fromItem, Map<String, String> aliasToTable, Set<String> tableNames) {
        if (fromItem instanceof net.sf.jsqlparser.schema.Table table) {
            String tableName = table.getName();
            tableNames.add(tableName);
            if (table.getAlias() != null) {
                aliasToTable.put(table.getAlias().getName(), tableName);
            }
        } else if (fromItem instanceof ParenthesedSelect parenthesedSelect) {
            Select subSelect = parenthesedSelect.getSelect();
            if (subSelect != null) {
                collectTableInfo(subSelect, aliasToTable, tableNames);
            }
        }
    }

    /**
     * 从SELECT语句中收集所有列引用
     */
    private void collectColumns(Select select, Set<Column> columns) {
        if (select instanceof PlainSelect plainSelect) {
            collectColumnsFromPlainSelect(plainSelect, columns);
        } else if (select instanceof SetOperationList setOp) {
            for (Select body : setOp.getSelects()) {
                collectColumns(body, columns);
            }
        }
    }

    private void collectColumnsFromPlainSelect(PlainSelect plainSelect, Set<Column> columns) {
        // SELECT 子句
        if (plainSelect.getSelectItems() != null) {
            for (SelectItem<?> item : plainSelect.getSelectItems()) {
                Expression expr = item.getExpression();
                if (expr != null) {
                    collectColumnsFromExpression(expr, columns);
                }
            }
        }

        // WHERE 子句
        if (plainSelect.getWhere() != null) {
            collectColumnsFromExpression(plainSelect.getWhere(), columns);
        }

        // GROUP BY 子句
        if (plainSelect.getGroupBy() != null && plainSelect.getGroupBy().getGroupByExpressionList() != null) {
            for (var expr : plainSelect.getGroupBy().getGroupByExpressionList()) {
                collectColumnsFromExpression((Expression) expr, columns);
            }
        }

        // HAVING 子句
        if (plainSelect.getHaving() != null) {
            collectColumnsFromExpression(plainSelect.getHaving(), columns);
        }

        // ORDER BY 子句
        if (plainSelect.getOrderByElements() != null) {
            for (OrderByElement order : plainSelect.getOrderByElements()) {
                collectColumnsFromExpression(order.getExpression(), columns);
            }
        }

        // JOIN ON 条件
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                Collection<Expression> onExps = join.getOnExpressions();
                if (onExps != null) {
                    for (Expression onExp : onExps) {
                        collectColumnsFromExpression(onExp, columns);
                    }
                }
            }
        }
    }

    /**
     * 递归从表达式中收集所有列引用
     */
    private void collectColumnsFromExpression(Expression expr, Set<Column> columns) {
        if (expr == null) {
            return;
        }

        // 列引用
        if (expr instanceof Column col) {
            columns.add(col);
            return;
        }

        // 二元表达式（AND, OR, 比较运算, 算术运算等）
        if (expr instanceof BinaryExpression binary) {
            collectColumnsFromExpression(binary.getLeftExpression(), columns);
            collectColumnsFromExpression(binary.getRightExpression(), columns);
            return;
        }

        // 函数调用：SUM(col), COUNT(col) 等
        if (expr instanceof Function func) {
            ExpressionList<?> params = func.getParameters();
            if (params != null && !params.isEmpty()) {
                for (var param : params) {
                    collectColumnsFromExpression(param, columns);
                }
            }
            return;
        }


        if (expr instanceof ParenthesedExpressionList<?> paren) {
            if (!paren.isEmpty()) {
                for (var innerExpr : paren) {
                    collectColumnsFromExpression(innerExpr, columns);
                }
            }
            return;
        }

        // CASE WHEN 表达式
        if (expr instanceof CaseExpression caseExpr) {
            if (caseExpr.getSwitchExpression() != null) {
                collectColumnsFromExpression(caseExpr.getSwitchExpression(), columns);
            }
            if (caseExpr.getWhenClauses() != null) {
                for (WhenClause when : caseExpr.getWhenClauses()) {
                    collectColumnsFromExpression(when.getWhenExpression(), columns);
                    collectColumnsFromExpression(when.getThenExpression(), columns);
                }
            }
            if (caseExpr.getElseExpression() != null) {
                collectColumnsFromExpression(caseExpr.getElseExpression(), columns);
            }
            return;
        }

        // BETWEEN 表达式
        if (expr instanceof Between between) {
            collectColumnsFromExpression(between.getLeftExpression(), columns);
            collectColumnsFromExpression(between.getBetweenExpressionStart(), columns);
            collectColumnsFromExpression(between.getBetweenExpressionEnd(), columns);
            return;
        }

        // IN 表达式
        if (expr instanceof InExpression inExpr) {
            collectColumnsFromExpression(inExpr.getLeftExpression(), columns);
            return;
        }

        // IS NULL 表达式
        if (expr instanceof IsNullExpression isNull) {
            collectColumnsFromExpression(isNull.getLeftExpression(), columns);
            return;
        }

        // NOT 表达式
        if (expr instanceof NotExpression not) {
            collectColumnsFromExpression(not.getExpression(), columns);

        }
    }

    /**
     * 解析表别名到真实表名
     *
     * @param alias        表别名，可能为null
     * @param aliasToTable 别名 -> 真实表名 映射
     * @param tableNames   所有表名
     * @return 真实表名，无法解析时返回null
     */
    private String resolveTableName(String alias, Map<String, String> aliasToTable, Set<String> tableNames) {
        if (alias == null || alias.isBlank()) {
            // 无别名修饰，如果SQL中只有一张表则直接返回
            if (tableNames.size() == 1) {
                return tableNames.iterator().next();
            }
            return null;
        }

        // 先查别名映射
        String resolved = aliasToTable.get(alias);
        if (resolved != null) {
            return resolved;
        }

        // 别名可能就是真实表名本身（没有使用AS）
        if (tableNames.contains(alias)) {
            return alias;
        }

        return null;
    }

    /**
     * 执行SQL的具体实现（待实现）
     *
     * @param modulePrefix 所属服务
     * @param dataSource   数据源
     * @param sql          最终执行的SQL
     * @return 查询结果列表，每行是一个 字段名->值 的Map
     */
    private SqlQueryVO doExecuteSql(String modulePrefix, String dataSource, String sql) {

        if (DeployUtils.isSingle()) {
            return SpringUtils.getBean(ISQLExecutionService.class).query(dataSource, sql, null);
        }

        String serviceName = DeployUtils.getDistributedServerModuleMapping()
                .get(modulePrefix);
        if (StringUtils.isBlank(serviceName)) {
            throw new AgentException("%s 服务未启动，请联系管理员".formatted(modulePrefix));
        }
        RestClient resultClient = restClientBuilder.clone()
                .baseUrl("http://%s".formatted(serviceName))
                .build();

        DistributedSqlQueryVO v = FeignUtils.data(resultClient.post()
                .uri(CoreConstants.EndPoint.ENDPOINT_DB_EXECUTION)
                .body(Map.of("datasource", dataSource,
                        "sql", sql))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {}));

        return new SqlQueryVO(v.executionSql ,v.transformationData());
    }

    //专门用于解析分布式部署时的结果，使用LinkedHashMap接收
     private record DistributedSqlQueryVO(
            String executionSql ,
            List<LinkedHashMap<String, Object>> data
    ){

        public List<Map<String, Object>> transformationData(){
            if(CollectionUtils.isEmpty(data)){
                return Collections.emptyList();
            }

            return data.stream()
                    .map(v ->(Map<String , Object>)v)
                    .toList();

        }

    }


    /**
     * 根据字段脱敏配置对查询结果进行脱敏处理
     * 基于SQL解析出的表名->列名映射精确定位每个字段所属的表，获取对应的脱敏配置
     *
     * @param queryResult              查询结果
     * @param userTableModelPermission 用户表模型权限
     * @param modulePrefix             所属服务
     * @param dataSource               数据源
     * @param tableColumnMap           SQL解析出的 表名->列名集合 映射
     * @return 已脱敏的字段名列表
     */
    private List<String> applyDesensitization(List<Map<String, Object>> queryResult,
                                              Map<String, Map<String, FieldPermission>> userTableModelPermission,
                                              String modulePrefix, String dataSource,
                                              Map<String, Set<String>> tableColumnMap) {
        if (CollectionUtils.isEmpty(queryResult) || tableColumnMap.isEmpty()) {
            return Collections.emptyList();
        }


        Map<String, FieldPermission> fieldPermMap = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : tableColumnMap.entrySet()) {
            String tableName = entry.getKey();
            Set<String> columnNames = entry.getValue();
            String key = modulePrefix + ":" + dataSource + ":" + tableName;
            Map<String, FieldPermission> tablePerms = userTableModelPermission.get(key);
            if (tablePerms == null || tablePerms.isEmpty()) {
                continue;
            }
            for (String colName : columnNames) {
                FieldPermission perm = tablePerms.get(colName);
                if (perm == null) {
                    continue;
                }
                FieldPermission existing = fieldPermMap.get(colName);
                if (existing == null) {
                    fieldPermMap.put(colName, perm);
                } else if (perm.desensitize() && !existing.desensitize()) {
                    // 如果当前表的该字段要求脱敏而之前的没有，使用脱敏配置
                    fieldPermMap.put(colName, perm);
                }
            }
        }

        List<String> desensitizedFields = new ArrayList<>();

        for (Map<String, Object> row : queryResult) {
            for (Map.Entry<String, Object> field : row.entrySet()) {
                String fieldName = field.getKey();
                FieldPermission perm = fieldPermMap.get(fieldName);
                if (perm == null || !perm.desensitize()) {
                    continue;
                }

                Object value = field.getValue();
                if (value == null) {
                    continue;
                }

                String desensitized = desensitize(value.toString(), perm);
                field.setValue(desensitized);

                if (!desensitizedFields.contains(fieldName)) {
                    desensitizedFields.add(fieldName);
                }
            }
        }

        return desensitizedFields;
    }

    /**
     * 根据脱敏策略对字符串值进行脱敏
     * 委托给 SensitiveStrategy.getConverter().apply() 处理，CUSTOM 策略调用 customConverter
     *
     * @param value 原始值
     * @param perm  字段权限配置（含脱敏策略）
     * @return 脱敏后的值
     */
    private String desensitize(String value, FieldPermission perm) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        if (perm.strategy() == org.quyq.gwsu.common.security.annotation.SensitiveStrategy.CUSTOM) {
            return org.quyq.gwsu.common.security.annotation.SensitiveStrategy.customConverter(
                    value,
                    perm.symbol() != null ? perm.symbol() : "*",
                    perm.prefixNoMaskLen() != null ? perm.prefixNoMaskLen() : 0,
                    perm.suffixNoMaskLen() != null ? perm.suffixNoMaskLen() : 0);
        }

        return perm.strategy().getConverter().apply(value);
    }

    /**
     * 获取当前用户对指定表的字段权限
     */
    private Map<String, FieldPermission> getCurrentUserFieldPermissions(DatabaseSearchAgent databaseSearchAgent, String modulePrefix, String dataSource, String tableName) {
        Map<String, Map<String, FieldPermission>> mergedPermissions = databaseSearchAgent.getUserTableModelPermission();

        String key = modulePrefix + ":" + dataSource + ":" + tableName;
        return mergedPermissions.getOrDefault(key, Map.of());
    }
}
