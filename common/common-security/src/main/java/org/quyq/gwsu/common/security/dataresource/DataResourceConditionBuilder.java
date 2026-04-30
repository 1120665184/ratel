package org.quyq.gwsu.common.security.dataresource;


import org.quyq.gwsu.common.security.domain.DataResoureRule;
import org.quyq.gwsu.common.security.enums.DataResourceAssertType;

import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/13
 * @description
 */
public interface DataResourceConditionBuilder <T , U>{

    Map<DataResourceAssertType , AssertTypeBuilder<T>> getAssertTypes();

    /**
     * 通过资源配置规则构建数据资源过滤条件
     * @param tableRule
     * @param alias
     * @param resourceScope
     * @return
     */
    U build(DataResoureRule tableRule , String alias , Map<String , List<?>> resourceScope);

}
