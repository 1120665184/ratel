package org.quyq.gwsu.common.security.captcha.properties;

import lombok.Data;
import org.quyq.gwsu.common.security.captcha.enums.CaptchaType;
import tools.jackson.databind.ObjectMapper;

import java.util.Properties;

/**
 * 验证码配置，对应 security_config.captcha_config。
 *
 * @author Quyq
 */
@Data
public class CaptchaProperties {

    public static final String CONFIG_KEY = "captcha_config";

    /**
     * 默认验证码类型。
     */
    private CaptchaType type = CaptchaType.CLICK_WORD;

    /**
     * 是否启用验证码能力。
     */
    private boolean enabled = true;

    /**
     * 滑动拼图底图路径。
     */
    private String jigsaw = "";

    /**
     * 文字点选底图路径。
     */
    private String picClick = "";

    /**
     * 水印文字。
     */
    private String waterMark = "Ratel-Manager";

    /**
     * 水印字体。
     */
    private String waterFont = "WenQuanZhengHei.ttf";

    /**
     * 点选文字字体。
     */
    private String fontType = "WenQuanZhengHei.ttf";

    /**
     * 滑块允许误差偏移量。
     */
    private String slipOffset = "5";

    /**
     * 是否开启坐标 AES 加密。
     */
    private Boolean aesStatus = true;

    /**
     * 滑块干扰项。
     */
    private String interferenceOptions = "0";

    /**
     * AJ-Captcha 本地缓存阈值。
     */
    private String cacheNumber = "1000";

    /**
     * AJ-Captcha 本地缓存清理周期，单位秒。
     */
    private String timingClear = "180";

    /**
     * 是否启用 AJ-Captcha 历史数据清理。
     */
    private boolean historyDataClearEnable = false;

    /**
     * 是否启用 AJ-Captcha 请求频率限制。
     */
    private boolean reqFrequencyLimitEnable = false;

    /**
     * 一分钟内 check 接口失败锁定阈值。
     */
    private int reqGetLockLimit = 5;

    /**
     * 失败锁定时间，单位秒。
     */
    private int reqGetLockSeconds = 300;

    /**
     * get 接口一分钟访问限制。
     */
    private int reqGetMinuteLimit = 100;

    /**
     * check 接口一分钟访问限制。
     */
    private int reqCheckMinuteLimit = 100;

    /**
     * verify 接口一分钟访问限制。
     */
    private int reqVerifyMinuteLimit = 100;

    /**
     * 点选字体样式。
     */
    private int fontStyle = 1;

    /**
     * 点选字体大小。
     */
    private int fontSize = 25;

    /**
     * 点选文字个数。
     */
    private int clickWordCount = 4;

    public CaptchaType effectiveType(CaptchaType requestType) {
        return requestType == null ? type : requestType;
    }

    public Properties toCaptchaServiceProperties() {
        return toCaptchaServiceProperties(effectiveType(null));
    }

    public Properties toCaptchaServiceProperties(CaptchaType type) {
        Properties properties = new Properties();
        CaptchaType captchaType = effectiveType(type);
        properties.put("captcha.cacheType", "redis");
        properties.put("captcha.water.mark", waterMark);
        properties.put("captcha.font.type", fontType);
        properties.put("captcha.type", captchaType.getCode());
        properties.put("captcha.interference.options", interferenceOptions);
        properties.put("captcha.captchaOriginalPath.jigsaw", jigsaw);
        properties.put("captcha.captchaOriginalPath.pic-click", picClick);
        properties.put("captcha.slip.offset", slipOffset);
        properties.put("captcha.aes.status", String.valueOf(Boolean.TRUE.equals(aesStatus)));
        properties.put("captcha.water.font", waterFont);
        properties.put("captcha.cache.number", cacheNumber);
        properties.put("captcha.timing.clear", timingClear);
        properties.put("captcha.history.data.clear.enable", historyDataClearEnable ? "1" : "0");
        properties.put("captcha.req.frequency.limit.enable", reqFrequencyLimitEnable ? "1" : "0");
        properties.put("captcha.req.get.lock.limit", String.valueOf(reqGetLockLimit));
        properties.put("captcha.req.get.lock.seconds", String.valueOf(reqGetLockSeconds));
        properties.put("captcha.req.get.minute.limit", String.valueOf(reqGetMinuteLimit));
        properties.put("captcha.req.check.minute.limit", String.valueOf(reqCheckMinuteLimit));
        properties.put("captcha.req.verify.minute.limit", String.valueOf(reqVerifyMinuteLimit));
        properties.put("captcha.font.size", String.valueOf(fontSize));
        properties.put("captcha.font.style", String.valueOf(fontStyle));
        properties.put("captcha.word.count", String.valueOf(clickWordCount));
        return properties;
    }


}
