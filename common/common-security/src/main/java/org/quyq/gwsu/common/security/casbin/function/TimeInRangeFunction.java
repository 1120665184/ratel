package org.quyq.gwsu.common.security.casbin.function;


import com.googlecode.aviator.runtime.type.AviatorBoolean;
import com.googlecode.aviator.runtime.type.AviatorObject;
import org.casbin.jcasbin.util.function.CustomFunction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/5/1
 * @description 表达式：判断当前时间是否在指定的时间范围内（包含起止时间）
 * 参数：arg1 - 开始时间，格式 yyyy-MM-dd HH:mm
 *      arg2 - 结束时间，格式 yyyy-MM-dd HH:mm
 */
public class TimeInRangeFunction extends CustomFunction {

    public static final String NAME = "timeInRange";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        String startStr = (String) arg1.getValue(env);
        String endStr = (String) arg2.getValue(env);

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = LocalDateTime.parse(startStr, FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endStr, FORMATTER);
            return AviatorBoolean.valueOf(!now.isBefore(start) && !now.isAfter(end));
        } catch (Exception e) {
            return AviatorBoolean.FALSE;
        }
    }
}
