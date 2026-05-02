package org.quyq.gwsu.common.security.casbin.function;


import com.googlecode.aviator.runtime.type.AviatorBoolean;
import com.googlecode.aviator.runtime.type.AviatorObject;
import org.casbin.jcasbin.util.function.CustomFunction;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/5/1
 * @description 表达式：判断当前时间是否在指定的时间范围内（包含起止时间）
 * 参数：arg1 - 当前时间（r.env.datatime），支持毫秒值/LocalDateTime/Date
 *      arg2 - 开始时间，格式 yyyy-MM-dd HH:mm
 *      arg3 - 结束时间，格式 yyyy-MM-dd HH:mm
 */
public class TimeInRangeFunction extends CustomFunction {

    public static final String NAME = "timeInRange";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3) {
        Object datatimeValue = arg1.getValue(env);
        String startStr = (String) arg2.getValue(env);
        String endStr = (String) arg3.getValue(env);

        try {
            LocalDateTime now = parseDateTime(datatimeValue);
            LocalDateTime start = LocalDateTime.parse(startStr, FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endStr, FORMATTER);
            return AviatorBoolean.valueOf(!now.isBefore(start) && !now.isAfter(end));
        } catch (Exception e) {
            return AviatorBoolean.FALSE;
        }
    }

    /**
     * 将 r.env.datatime 的值解析为 LocalDateTime
     * 支持：毫秒值(Long/Number)、LocalDateTime、Date
     */
    static LocalDateTime parseDateTime(Object value) {
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (value instanceof Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        if (value instanceof Number number) {
            return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(number.longValue()),
                    ZoneId.systemDefault());
        }
        if (value instanceof String str) {
            // 尝试解析为毫秒值
            try {
                long millis = Long.parseLong(str);
                return LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(millis),
                        ZoneId.systemDefault());
            } catch (NumberFormatException ignored) {
            }
            // 尝试解析为日期时间字符串
            try {
                return LocalDateTime.parse(str, FORMATTER);
            } catch (Exception ignored) {
            }
        }
        throw new IllegalArgumentException("不支持的时间类型: " + value.getClass().getName());
    }
}
