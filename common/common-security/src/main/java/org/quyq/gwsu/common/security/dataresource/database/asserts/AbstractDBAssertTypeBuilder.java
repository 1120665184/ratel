package org.quyq.gwsu.common.security.dataresource.database.asserts;


import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import org.quyq.gwsu.common.security.dataresource.AssertTypeBuilder;

/**
 * @author Quyq
 * @date 2026/4/13
 * @description
 */
public abstract class AbstractDBAssertTypeBuilder implements AssertTypeBuilder<Expression> {

    protected Expression getValue(Object value) {
        if (value instanceof Number || value.getClass().isPrimitive())
            return new LongValue(Long.parseLong(value.toString()));

        return new StringValue(value.toString());

    }
}
