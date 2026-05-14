package org.quyq.gwsu.common.security.domain;

import org.quyq.gwsu.common.security.annotation.SensitiveStrategy;

/**
 * 字段权限配置
 *
 * @param show            是否允许查询
 * @param desensitize     是否脱敏
 * @param strategy        脱敏策略
 * @param prefixNoMaskLen 自定义脱敏-不脱敏前缀长度
 * @param suffixNoMaskLen 自定义脱敏-不脱敏后缀长度
 * @param symbol          自定义脱敏-脱敏标识符
 */
public record FieldPermission(
        boolean show,
        boolean desensitize,
        SensitiveStrategy strategy,
        Integer prefixNoMaskLen,
        Integer suffixNoMaskLen,
        String symbol
) {
    /**
     * 默认权限：可查询、不脱敏
     */
    public static FieldPermission defaultPermission() {
        return new FieldPermission(true, false, SensitiveStrategy.NONE, null, null, null);
    }
}
