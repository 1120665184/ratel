package org.quyq.gwsu.common.database.metadata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 外键信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForeignKeyInfo {
    /**
     * 外键名称
     */
    private String name;

    /**
     * 当前表（外键所在表）的列名
     */
    private String columnName;

    /**
     * 引用表名
     */
    private String referencedTableName;

    /**
     * 引用表的列名
     */
    private String referencedColumnName;

    /**
     * 外键所在表名
     */
    private String tableName;

    /**
     * 更新规则
     * @see java.sql.DatabaseMetaData#importedKeyCascade
     * @see java.sql.DatabaseMetaData#importedKeyRestrict
     * @see java.sql.DatabaseMetaData#importedKeySetNull
     * @see java.sql.DatabaseMetaData#importedKeyNoAction
     * @see java.sql.DatabaseMetaData#importedKeySetDefault
     */
    private Integer updateRule;

    /**
     * 删除规则
     */
    private Integer deleteRule;

    /**
     * 外键在表中的顺序位置（从1开始）
     */
    private Integer keySeq;
}
