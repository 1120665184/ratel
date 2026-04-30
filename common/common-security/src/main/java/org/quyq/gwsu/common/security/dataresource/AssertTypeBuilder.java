package org.quyq.gwsu.common.security.dataresource;


import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/13
 * @description
 */
public interface AssertTypeBuilder<T> {

    /**
     * 构建条件实现
     *
     * @param field
     * @param values
     * @return
     */
    T toCondition(String field, List<Object> values);

}
