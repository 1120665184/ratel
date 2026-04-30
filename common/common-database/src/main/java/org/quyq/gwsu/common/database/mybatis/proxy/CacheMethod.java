package org.quyq.gwsu.common.database.mybatis.proxy;


import java.lang.reflect.Method;


/**
 * @author Quyq
 * @date 2026/3/19
 * @description
 */

public record CacheMethod(
        String databaseId,
        Method method
) {


}
