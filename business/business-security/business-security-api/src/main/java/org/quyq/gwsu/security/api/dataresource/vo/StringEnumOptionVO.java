package org.quyq.gwsu.security.api.dataresource.vo;

/**
 * 字符串值枚举选项 VO，用于前端下拉选择等场景
 *
 * @param label 显示文本
 * @param value 枚举值（字符串类型）
 * @author Quyq
 */
public record StringEnumOptionVO(String label, String value) {
}
