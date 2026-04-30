package org.quyq.gwsu.common.security.casbin.function;


import com.googlecode.aviator.runtime.type.AviatorBoolean;
import com.googlecode.aviator.runtime.type.AviatorObject;
import jodd.util.ArraysUtil;
import org.casbin.jcasbin.util.function.CustomFunction;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/4/5
 * @description 表达式：监测列表，数组，Map 或字符串中是否包含指定值
 */
public class ContainsFunction extends CustomFunction {

    public static final String NAME = "contains";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {

        Object roles = arg1.getValue(env);

        if (Objects.isNull(roles)) {
            return AviatorBoolean.FALSE;
        }

        Object targetVal = arg2.getValue(env);

        if (roles instanceof Collection<?> collection) {
            return AviatorBoolean.valueOf(collection.contains(targetVal));
        } else if (roles instanceof Map<?, ?> map) {
            return AviatorBoolean.valueOf(map.containsKey(targetVal));
        } else if (roles.getClass().isArray()) {
            return AviatorBoolean.valueOf(ArraysUtil.contains((Object[]) roles, targetVal));
        } else if (roles instanceof String str) {
            return AviatorBoolean.valueOf(str.contains(targetVal.toString()));
        }


        return AviatorBoolean.FALSE;
    }
}
