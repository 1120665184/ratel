package org.quyq.gwsu.common.security.enums;


import lombok.Getter;

/**
 * @author Quyq
 * @date 2026/4/13
 * @description
 */
@Getter
public enum DataResourceAssertType {

    /**
     * 权限数据单条时 = 查询，多条时 IN查询
     */
    EQ,

    /**
     * LIKE查询
     */
    LIKE,
//    /**
//     * 如果字段中是,分隔的字符串的值，可以使用该模式
//     * mysql会使用 find_in_set函数进行查询
//     */
//    SET


    ;


}
