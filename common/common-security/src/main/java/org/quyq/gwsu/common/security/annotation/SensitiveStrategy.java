package org.quyq.gwsu.common.security.annotation;

import lombok.Getter;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * 脱敏策略枚举
 */
@Getter
public enum SensitiveStrategy {
    /** 无脱敏 */
    NONE(UnaryOperator.identity()),
    /** 用户名：张** */
    USERNAME(s ->s.replaceAll("(\\S)\\S*", "$1**")),
    /** 身份证：3301**********1234 */
    ID_CARD(s -> s.replaceAll("(\\d{4})\\d{10}(\\w{4})","$1**********$2")),
    /** 手机号：138****1234 */
    PHONE(s -> s.replaceAll("(\\d{3})\\d{4}(\\d{4})","$1****$2")),
    /** 邮箱：a****b@example.com */
    EMAIL(s -> s.replaceAll("(\\w?)(\\w+)(\\w)(@\\w+(\\.[a-z+])?)","$1****$3$4")),
    /** 地址：浙江省****杭州市**** */
    ADDRESS(s -> s.replaceAll("(\\S{3})\\S{2}(\\S*)\\S{2}","$1****$2****")),
    /** 自定义脱敏 */
    CUSTOM(UnaryOperator.identity());

    SensitiveStrategy(UnaryOperator<String> converter){
        this.converter = converter;
    }
    private final UnaryOperator<String> converter;



    /**
     * 自定义脱敏文本
     * @param str
     * @param symbol
     * @param prefixNoMaskLen
     * @param suffixNoMaskLen
     * @return
     */
    public static String customConverter(String str,String symbol,int prefixNoMaskLen,int suffixNoMaskLen){
        if(Objects.isNull(symbol)){
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0 ,n = str.length();i < n; i++ ){
            if(i < prefixNoMaskLen || i > (n - suffixNoMaskLen - 1)){
                sb.append(str.charAt(i));
                continue;
            }
            sb.append(symbol);
        }
        return sb.toString();
    }
}
