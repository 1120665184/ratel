package org.quyq.gwsu.common.database.metadata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据库/Schema 信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseInfo {
    /**
     * 数据库/Schema 名称
     */
    private String name;

    /**
     * 备注/注释
     */
    private String remark;

    /**
     * 字符集
     */
    private String charset;

    /**
     * 排序规则
     */
    private String collation;
}
