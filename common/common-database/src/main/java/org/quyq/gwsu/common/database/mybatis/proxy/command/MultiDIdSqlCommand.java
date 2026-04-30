package org.quyq.gwsu.common.database.mybatis.proxy.command;


import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Method;

/**
 * @author Quyq
 * @date 2026/3/19
 * @description
 */
public class MultiDIdSqlCommand {

    private final String name;
    private final SqlCommandType type;


    public MultiDIdSqlCommand(Configuration configuration, Class<?> mapperInterface, Method method, String databaseId) {
        final String methodName = method.getName();
        final Class<?> declaringClass = method.getDeclaringClass();
        MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass, configuration, databaseId);
        if (ms == null) {
            if (method.getAnnotation(Flush.class) == null) {
                throw new BindingException(
                        "Invalid bound statement (not found): " + mapperInterface.getName() + "." + methodName);
            }
            name = null;
            type = SqlCommandType.FLUSH;
        } else {
            name = ms.getId();
            type = ms.getSqlCommandType();
            if (type == SqlCommandType.UNKNOWN) {
                throw new BindingException("Unknown execution method for: " + name);
            }
        }
    }

    public String getName() {
        return name;
    }

    public SqlCommandType getType() {
        return type;
    }

    private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName, Class<?> declaringClass,
                                                   Configuration configuration, String databaseId) {
        String statementId = mapperInterface.getName() + "." + methodName;
        String finStaId = String.format("%s_%s", statementId, databaseId);

        if (configuration.hasStatement(finStaId)) {
            return configuration.getMappedStatement(finStaId);
        } else if (configuration.hasStatement(statementId)) {
            return configuration.getMappedStatement(statementId);
        }
        if (mapperInterface.equals(declaringClass)) {
            return null;
        }
        for (Class<?> superInterface : mapperInterface.getInterfaces()) {
            if (declaringClass.isAssignableFrom(superInterface)) {
                MappedStatement ms = resolveMappedStatement(superInterface, methodName, declaringClass, configuration, databaseId);
                if (ms != null) {
                    return ms;
                }
            }
        }
        return null;
    }


}
