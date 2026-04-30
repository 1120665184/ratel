package org.quyq.gwsu.common.database.mybatis.proxy;


import lombok.Getter;
import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Quyq
 * @date 2026/3/19
 * @description
 */
public class MultiDIdMybatisMapperProxyFactory<T> {

    @Getter
    private final Class<T> mapperInterface;
    @Getter
    private final Map<CacheMethod, MultiDIdMybatisMapperProxy.MapperMethodInvoker> methodCache = new ConcurrentHashMap<>();

    public MultiDIdMybatisMapperProxyFactory(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    @SuppressWarnings("unchecked")
    protected T newInstance(MultiDIdMybatisMapperProxy<T> mapperProxy) {
        return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[]{mapperInterface}, mapperProxy);
    }

    public T newInstance(SqlSession sqlSession) {
        final MultiDIdMybatisMapperProxy<T> mapperProxy = new MultiDIdMybatisMapperProxy<>(sqlSession, mapperInterface, methodCache);
        return newInstance(mapperProxy);
    }
}
