package org.quyq.gwsu.common.security.domain.vo;


import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/5/16
 * @description
 */
public record SqlQueryVO (
        String executionSql ,
        List<Map<String, Object>> data
){
}
