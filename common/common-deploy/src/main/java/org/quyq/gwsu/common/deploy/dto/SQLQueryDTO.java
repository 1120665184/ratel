package org.quyq.gwsu.common.deploy.dto;


import java.util.List;

/**
 * @author Quyq
 * @date 2026/5/16
 * @description sql查询dto
 */
public record SQLQueryDTO(
        String datasource ,
        String sql ,
        List<Object> params
) {
}
