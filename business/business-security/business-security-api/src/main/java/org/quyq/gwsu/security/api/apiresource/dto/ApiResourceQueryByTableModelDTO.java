package org.quyq.gwsu.security.api.apiresource.dto;


/**
 * @author Quyq
 * @date 2026/5/18
 * @description
 */
public record ApiResourceQueryByTableModelDTO(
        String modulePrefix ,
        String datasource ,
        String tableName
) {
}
