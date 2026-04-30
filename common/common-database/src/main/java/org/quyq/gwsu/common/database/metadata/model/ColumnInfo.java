package org.quyq.gwsu.common.database.metadata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 列信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnInfo {
    /**
     * 列名
     */
    private String name;

    /**
     * 数据类型
     */
    private String type;

    /**
     * 是否允许空值
     */
    private Boolean nullable;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 列注释
     */
    private String remark;

    /**
     * 列位置
     */
    private Integer position;

    /**
     * 长度
     */
    private Integer length;

    /**
     * 精度
     */
    private Integer precision;

    /**
     * 小数位数
     */
    private Integer scale;

    /**
     * 是否主键
     */
    private Boolean isPrimaryKey;

    /**
     * 数值类型集合
     */
    private static final Set<String> NUMERIC_TYPES = Set.of(
            "TINYINT", "SMALLINT", "MEDIUMINT", "INT", "INTEGER", "BIGINT",
            "FLOAT", "DOUBLE", "DECIMAL", "NUMERIC", "REAL", "BIT",
            "SMALLSERIAL", "SERIAL", "BIGSERIAL",
            "NUMBER", "MONEY", "CURRENCY"
    );

    /**
     * 文本类型集合
     */
    private static final Set<String> TEXT_TYPES = Set.of(
            "CHAR", "VARCHAR", "TEXT", "TINYTEXT", "MEDIUMTEXT", "LONGTEXT",
            "NCHAR", "NVARCHAR", "NTEXT",
            "CLOB", "NCLOB",
            "STRING", "BPCHAR", "NAME"
    );

    /**
     * 判断是否为数值类型
     *
     * @return true-数值类型，false-非数值类型
     */
    public boolean isNumeric() {
        if (type == null) {
            return false;
        }
        String upperType = type.toUpperCase();
        // 处理带参数的类型，如 DECIMAL(10,2)
        String baseType = upperType.split("\\(")[0].trim();
        return NUMERIC_TYPES.contains(baseType);
    }

    /**
     * 判断是否为文本类型
     *
     * @return true-文本类型，false-非文本类型
     */
    public boolean isText() {
        if (type == null) {
            return false;
        }
        String upperType = type.toUpperCase();
        // 处理带参数的类型，如 VARCHAR(255)
        String baseType = upperType.split("\\(")[0].trim();
        return TEXT_TYPES.contains(baseType);
    }
}
