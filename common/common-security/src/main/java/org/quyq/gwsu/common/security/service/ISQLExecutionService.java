package org.quyq.gwsu.common.security.service;


import org.quyq.gwsu.common.security.domain.vo.SqlQueryVO;

import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/5/16
 * @description
 */
public interface ISQLExecutionService {


    /**
     * 执行查询 SQL
     *
     * @param datasource
     * @param sql
     * @param parameters
     * @return
     */
    SqlQueryVO query(String datasource, String sql, List<Object> parameters);

}
