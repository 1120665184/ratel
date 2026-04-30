package org.quyq.gwsu.common.cache.utils;


import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/3/20
 * @description 全局ID生成工具类 , 适合用于生成业务编号的唯一不重复部分
 */

@RequiredArgsConstructor
public class IDGenerationUtils {

    private final CacheUtils cacheUtils;


    //2026-01-01 00:00:00 秒值
    private static final long START_OFFSET = 1767225600L;

    private static final int DEF_COUNT_BITS = 12;


    //id key前缀
    private static final String ORDER_COUNT_KEY = "id:";
    //账单记录key
    private static final String BORROW_TIME_KEY = "borrow_time_key";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd:HH:mm");


    public String generateNextIdStr() {
        return String.valueOf(generateNextId());
    }


    public String generateNextIdStr(String name) {
        return generateNextIdStr(name, DEF_COUNT_BITS);
    }

    public String generateNextIdStr(int countBits) {
        return generateNextIdStr(null, countBits);
    }

    public String generateNextIdStr(String name, int countBits) {
        return String.valueOf(generateNextId(name, countBits));
    }

    public long generateNextId() {
        return generateNextId(null, DEF_COUNT_BITS);
    }

    public long generateNextId(int countBits) {
        return generateNextId(null, countBits);
    }

    /**
     * 生成ID
     *
     * @param name      注意：名称相同不会生成重复值，不同时会生成重复值
     * @param countBits 可以控制生成id的长度 ， 值小于32
     *                  注意：确保生成ID唯一的场景，需要name和countBits始终保持不变
     * @return
     */
    public long generateNextId(String name, int countBits) {

        if (countBits > 31) {
            throw new IllegalArgumentException("countBits must be less than 31");
        }

        long currentTimeSeconds = getCurrentTimeSeconds();

        LocalDateTime now = LocalDateTime.ofEpochSecond(currentTimeSeconds, 0, ZoneOffset.UTC);

        String dateKey = now.format(DATE_TIME_FORMATTER);

        // 获取当前时间戳（秒）
        long timeStamp = currentTimeSeconds - START_OFFSET;

        String key = ORDER_COUNT_KEY + dateKey;
        if (StringUtils.hasText(name)) {
            key = "%s:%s".formatted(key, name);
        }


        List<String> keys = new ArrayList<>();
        keys.add(key);
        //记录账单key
        keys.add(StringUtils.hasText(name) ? "%s:%s".formatted(BORROW_TIME_KEY, name) : BORROW_TIME_KEY);

        //加载脚本
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/increment.lua")));
        script.setResultType(String.class);

        //获取自增值和时间账单 格式： 自增值_时间账单
        Optional<String> incrementAndBorrow = Optional.ofNullable(
                cacheUtils.withRebel(
                        () -> cacheUtils.executeScript(script, new StringRedisSerializer(), keys,
                                timeStamp, 7200, countBits)
                )
        );
        if (incrementAndBorrow.isEmpty()) {
            throw new IllegalArgumentException("自增ID获取失败");
        }
        String[] tmp = incrementAndBorrow.get().split("_");
        int increment = Integer.parseInt(tmp[0]);
        int borrow = Integer.parseInt(tmp[1]);

        return (timeStamp + borrow) << countBits | increment;

    }

    /**
     * 从redis获取当前秒值
     * 从redis获取是考虑到，分布式部署应用后，可能不同服务器的时间有误差
     *
     * @return
     */
    private long getCurrentTimeSeconds() {
        DefaultRedisScript<Long> getTimeScript = new DefaultRedisScript<>("return tonumber(redis.call('TIME')[1])", Long.class);

        return cacheUtils.executeScript(getTimeScript, Collections.singletonList("ignore"));
    }


}
