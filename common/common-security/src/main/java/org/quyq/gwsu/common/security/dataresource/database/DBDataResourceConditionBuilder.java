package org.quyq.gwsu.common.security.dataresource.database;


import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.security.dataresource.AssertTypeBuilder;
import org.quyq.gwsu.common.security.dataresource.DataResourceConditionBuilder;
import org.quyq.gwsu.common.security.dataresource.database.asserts.EQAssertTypeBuilder;
import org.quyq.gwsu.common.security.dataresource.database.asserts.LikeAssertTypeBuilder;
import org.quyq.gwsu.common.security.domain.DataResoureRule;
import org.quyq.gwsu.common.security.enums.DataResourceAssertType;
import org.quyq.gwsu.common.security.enums.DataResourceFieldConditionType;
import org.quyq.gwsu.common.security.exception.SecurityException;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/4/13
 * @description 数据库数据资源条件过滤表达式构建实现
 */
public class DBDataResourceConditionBuilder implements DataResourceConditionBuilder<Expression, Expression> {
    @Override
    public Map<DataResourceAssertType, AssertTypeBuilder<Expression>> getAssertTypes() {
        return Map.of(
                DataResourceAssertType.EQ, new EQAssertTypeBuilder(),
                DataResourceAssertType.LIKE, new LikeAssertTypeBuilder()
        );
    }

    @Override
    public Expression build(DataResoureRule tableRule, String alias, Map<String, List<?>> resourceScope) {
        if (CollectionUtils.isEmpty(tableRule.getConditions())) {
            return null;
        }

        Expression ex = null;
        for (DataResoureRule.FieldCondition condition : tableRule.getConditions()) {
            AssertTypeBuilder<Expression> expressionBuilder = getAssertTypes().get(condition.getAssertType());
            if (Objects.isNull(expressionBuilder)) {
                throw new SecurityException(CommonErrorCode.E03003, "缺少【%s】类型的数据资源条件构造器，请联系管理员".formatted(condition.getAssertType()));
            }
            String field = Objects.nonNull(alias) ? String.format("%s.%s", alias, condition.getFieldName()) : condition.getFieldName();
            Expression curExp = null;
            //构建数据
            List<?> scopeDatas = condition.getUserResourceFields().stream()
                    .map(resourceScope::get)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream).toList();

            if (!CollectionUtils.isEmpty(scopeDatas)) {
                curExp = expressionBuilder.toCondition(field, (List<Object>) scopeDatas);
            }
            if (condition.isShowNull()) {
                curExp = addShowNull(field, curExp);
            }


            if (Objects.isNull(ex)) {
                ex = curExp;
            } else {

                ex = DataResourceFieldConditionType.OR == condition.getRelationship() ? new OrExpression(ex, curExp) : new AndExpression(ex, curExp);
            }

        }


        return new ParenthesedExpressionList<>(ex);
    }


    private Expression addShowNull(String field, Expression condition) {
        IsNullExpression isNullExpression = new IsNullExpression();
        isNullExpression.setLeftExpression(new Column(field));
        if (Objects.isNull(condition)) {
            return isNullExpression;
        }


        return new ParenthesedExpressionList<>(new OrExpression(condition, isNullExpression));
    }

}
