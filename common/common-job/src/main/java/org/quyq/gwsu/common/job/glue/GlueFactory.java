package org.quyq.gwsu.common.job.glue;

import org.quyq.gwsu.common.job.handler.IJobHandler;
import org.quyq.gwsu.common.job.glue.impl.SpringGlueFactory;
import groovy.lang.GroovyClassLoader;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Glue工厂，根据代码源生成类/对象
 */
public class GlueFactory {

    private static GlueFactory glueFactory = new GlueFactory();

    public static GlueFactory getInstance() {
        return glueFactory;
    }

    /**
     * 根据类型刷新实例
     *
     * @param type 0-非Spring, 1-Spring
     */
    public static void refreshInstance(int type) {
        if (type == 0) {
            glueFactory = new GlueFactory();
        } else if (type == 1) {
            glueFactory = new SpringGlueFactory();
        }
    }

    /**
     * Groovy类加载器
     */
    private final GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
    private final ConcurrentMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();

    /**
     * 加载新实例（原型模式）
     *
     * @param codeSource 代码源
     * @return IJobHandler
     */
    public IJobHandler loadNewInstance(String codeSource) throws Exception {
        if (codeSource != null && !codeSource.trim().isEmpty()) {
            Class<?> clazz = getCodeSourceClass(codeSource);
            if (clazz != null) {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                if (instance instanceof IJobHandler) {
                    this.injectService(instance);
                    return (IJobHandler) instance;
                } else {
                    throw new IllegalArgumentException(">>>>>>>>>>> xxl-glue, loadNewInstance error, "
                            + "cannot convert from instance[" + instance.getClass() + "] to IJobHandler");
                }
            }
        }
        throw new IllegalArgumentException(">>>>>>>>>>> xxl-glue, loadNewInstance error, instance is null");
    }

    private Class<?> getCodeSourceClass(String codeSource) {
        try {
            byte[] md5 = MessageDigest.getInstance("MD5").digest(codeSource.getBytes());
            String md5Str = new BigInteger(1, md5).toString(16);

            Class<?> clazz = CLASS_CACHE.get(md5Str);
            if (clazz == null) {
                clazz = groovyClassLoader.parseClass(codeSource);
                CLASS_CACHE.putIfAbsent(md5Str, clazz);
            }
            return clazz;
        } catch (Exception e) {
            return groovyClassLoader.parseClass(codeSource);
        }
    }

    /**
     * 注入Bean字段的服务
     *
     * @param instance 实例
     */
    public void injectService(Object instance) {
        // do something
    }

}
