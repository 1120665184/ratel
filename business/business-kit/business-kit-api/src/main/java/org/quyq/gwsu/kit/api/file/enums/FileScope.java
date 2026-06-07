package org.quyq.gwsu.kit.api.file.enums;

/**
 * @author Quyq
 * @date 2026/5/14
 * @description 文件作用域
 */
public enum FileScope {

    /**
     * 公共的，无需认证便可访问
     */
    PUBLIC,
    /**
     * 受保护的，访问需要认证
     */
    PROTECTED,
    /**
     * 私有的，只有指定人员可访问
     */
    PRIVATE



}
