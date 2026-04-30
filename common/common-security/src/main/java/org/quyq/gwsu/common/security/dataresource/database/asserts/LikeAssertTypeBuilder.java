package org.quyq.gwsu.common.security.dataresource.database.asserts;


import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/13
 * @description
 */
public class LikeAssertTypeBuilder extends AbstractDBAssertTypeBuilder {

    @Override
    public Expression toCondition(String field, List<Object> values) {
        if (values.size() == 1) {
            return oneLikeCondition(field, values.getFirst());
        }
        // 构建 OrExpression 的逻辑不变
        Expression condition = values.stream()
                .map(val -> oneLikeCondition(field, val))
                .reduce(OrExpression::new)
                .orElseThrow(() -> new IllegalArgumentException("Values list cannot be empty"));

        // 使用 ParenthesedExpressionList 替代 Parenthesis
        ParenthesedExpressionList<Expression> parenthesedList = new ParenthesedExpressionList<>();
        parenthesedList.set(0, condition); // 或使用 addExpression(condition) 等方法

        return parenthesedList;
    }

    private static Expression oneLikeCondition(String field, Object value) {
        return new LikeExpression().withLeftExpression(new Column(field)).withRightExpression(new StringValue("%" + value.toString() + "%"));
    }

}
