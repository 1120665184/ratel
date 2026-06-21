package org.quyq.gwsu.common.security.casbin.function;


import com.googlecode.aviator.runtime.type.AviatorBoolean;
import com.googlecode.aviator.runtime.type.AviatorObject;
import org.casbin.jcasbin.util.function.CustomFunction;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;
import org.quyq.gwsu.common.security.domain.Subject;

import java.util.Map;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/4/17
 * @description
 */
public class IsUserLoginFunction extends CustomFunction {

    public static final String NAME = "isUserLogin";

    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public AviatorObject call(Map<String, Object> env) {
        Subject<Visitor> subject = (Subject<Visitor>) env.get("r_sub");
        if (Objects.isNull(subject)) {
            return AviatorBoolean.FALSE;
        }
        return AviatorBoolean.valueOf(subject.getDetail() instanceof UserInfo);
    }
}
