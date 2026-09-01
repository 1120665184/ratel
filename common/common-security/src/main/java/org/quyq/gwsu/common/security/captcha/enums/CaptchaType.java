package org.quyq.gwsu.common.security.captcha.enums;

import lombok.Getter;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.security.exception.SecurityException;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 验证码类型。
 *
 * @author Quyq
 */
@Getter
public enum CaptchaType {

    /**
     * 滑块拼图验证码。
     */
    BLOCK_PUZZLE("blockPuzzle", "滑块拼图验证码"),

    /**
     * 文字点选验证码。
     */
    CLICK_WORD("clickWord", "文字点选验证码"),
    ;

    private final String code;

    private final String name;

    CaptchaType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static CaptchaType from(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value) || type.code.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new SecurityException(CommonErrorCode.E04012));
    }
}
