package org.quyq.gwsu.common.database.metadata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableInfo {
    /**
     * 表名
     */
    private String name;

    /**
     * 表注释
     */
    private String remark;

    /**
     * 表类型（TABLE/VIEW等）
     */
    private String type;

    /**
     * 所属 Schema
     */
    private String schema;

    /**
     * 所属 Catalog
     */
    private String catalog;
}
