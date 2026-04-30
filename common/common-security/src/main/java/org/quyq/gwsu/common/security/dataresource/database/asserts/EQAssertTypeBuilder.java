package org.quyq.gwsu.common.security.dataresource.database.asserts;


import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/13
 * @description
 */
public class EQAssertTypeBuilder extends AbstractDBAssertTypeBuilder {

    @Override
    public Expression toCondition(String field, List<Object> values) {
        if (values.size() == 1) {
            return new EqualsTo(new Column(field), getValue(values.getFirst()));
        }
        ParenthesedExpressionList<Expression> finV = new ParenthesedExpressionList<>();
        values.forEach(v -> finV.add(getValue(v)));

        return new InExpression(new Column(field), finV);
    }
}
