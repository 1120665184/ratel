package org.quyq.gwsu.common.database.mybatis.proxy;

import com.baomidou.mybatisplus.core.metadata.MapperProxyMetadata;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.MybatisUtils;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.util.MapUtil;
import org.quyq.gwsu.common.core.utils.SpringUtils;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/3/19
 * @description
 */
public class MultiDIdMybatisMapperProxy<T> implements InvocationHandler, Serializable {

    private static final long serialVersionUID = -4724728412955527868L;
    private static final int ALLOWED_MODES = MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED
            | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC;
    private static final Constructor<MethodHandles.Lookup> lookupConstructor;
    private static final Method privateLookupInMethod;
    private final SqlSession sqlSession;
    private final Class<T> mapperInterface;
    private final Map<CacheMethod, MapperMethodInvoker> methodCache;

    public MultiDIdMybatisMapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<CacheMethod, MapperMethodInvoker> methodCache) {
        this.sqlSession = sqlSession;
        this.mapperInterface = mapperInterface;
        this.methodCache = methodCache;
    }

    static {
        Method privateLookupIn;
        try {
            privateLookupIn = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
        } catch (NoSuchMethodException e) {
            privateLookupIn = null;
        }
        privateLookupInMethod = privateLookupIn;

        Constructor<MethodHandles.Lookup> lookup = null;
        if (privateLookupInMethod == null) {
            // JDK 1.8
            try {
                lookup = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                lookup.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                        "There is neither 'privateLookupIn(Class, Lookup)' nor 'Lookup(Class, int)' method in java.lang.invoke.MethodHandles.",
                        e);
            } catch (Exception e) {
                lookup = null;
            }
        }
        lookupConstructor = lookup;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, args);
            }
            return cachedInvoker(method).invoke(proxy, method, args, sqlSession);
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
    }

    private MapperMethodInvoker cachedInvoker(Method method) throws Throwable {
        try {
            return MapUtil.computeIfAbsent(methodCache, new CacheMethod(databaseId(), method), m -> {
                if (!m.method().isDefault()) {
                    return new PlainMethodInvoker(new MultiDIdMapperMethod(mapperInterface, method, sqlSession.getConfiguration(), m.databaseId()));
                }
                try {
                    if (privateLookupInMethod == null) {
                        return new DefaultMethodInvoker(getMethodHandleJava8(method));
                    }
                    return new DefaultMethodInvoker(getMethodHandleJava9(method));
                } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                         | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException re) {
            Throwable cause = re.getCause();
            throw cause == null ? re : cause;
        }
    }

    public SqlSession getSqlSession() {
        return sqlSession;
    }

    public Class<T> getMapperInterface() {
        return mapperInterface;
    }

    private MethodHandle getMethodHandleJava9(Method method)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Class<?> declaringClass = method.getDeclaringClass();
        return ((MethodHandles.Lookup) privateLookupInMethod.invoke(null, declaringClass, MethodHandles.lookup())).findSpecial(
                declaringClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                declaringClass);
    }

    private MethodHandle getMethodHandleJava8(Method method)
            throws IllegalAccessException, InstantiationException, InvocationTargetException {
        final Class<?> declaringClass = method.getDeclaringClass();
        return lookupConstructor.newInstance(declaringClass, ALLOWED_MODES).unreflectSpecial(method, declaringClass);
    }


    private String databaseId() {
        DataSource datasource = SpringUtils.getBean(DataSource.class);
        DatabaseIdProvider provider = SpringUtils.getBean(DatabaseIdProvider.class);
        try {
            return provider.getDatabaseId(datasource);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    interface MapperMethodInvoker {
        Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable;
    }

    private static class PlainMethodInvoker implements MapperMethodInvoker {
        private final MultiDIdMapperMethod mapperMethod;

        public PlainMethodInvoker(MultiDIdMapperMethod mapperMethod) {
            this.mapperMethod = mapperMethod;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
            return mapperMethod.execute(sqlSession, args);
        }
    }

    private static class DefaultMethodInvoker implements MapperMethodInvoker {
        private final MethodHandle methodHandle;

        public DefaultMethodInvoker(MethodHandle methodHandle) {
            super();
            this.methodHandle = methodHandle;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
            boolean hasIgnoreStrategy = InterceptorIgnoreHelper.hasIgnoreStrategy();
            if (hasIgnoreStrategy) {
                return methodHandle.bindTo(proxy).invokeWithArguments(args);
            } else {
                try {
                    MapperProxyMetadata mapperProxyMetadata = MybatisUtils.getMapperProxy(proxy);
                    Class<?> mapperInterface = mapperProxyMetadata.getMapperInterface();
                    IgnoreStrategy ignoreStrategy = InterceptorIgnoreHelper.findIgnoreStrategy(mapperInterface, method);
                    if (ignoreStrategy == null) {
                        ignoreStrategy = IgnoreStrategy.builder().build();
                    }
                    InterceptorIgnoreHelper.handle(ignoreStrategy);
                    return methodHandle.bindTo(proxy).invokeWithArguments(args);
                } finally {
                    InterceptorIgnoreHelper.clearIgnoreStrategy();
                }
            }
        }
    }
}
