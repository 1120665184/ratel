package org.quyq.gwsu.common.core.utils;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.CharSequenceUtil;
import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.ArgumentException;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;

/**
 * 统一参数验证工具类 ， 用于字段非空等校验
 */
public class AssertUtils {

    private AssertUtils() {
    }


    /**
     * 断言字符串不为空
     *
     * @param value
     * @param error
     * @param <T>
     */
    public static <T extends CharSequence> T hasText(T value, ReturnCode error) {
        return hasText(value, error, null);
    }

    public static <T extends CharSequence> T hasText(T value, ReturnCode error, String message, Object... args) {
        if (!StringUtils.hasText(message)) {
            return Assert.notBlank(value, () -> new ArgumentException(error));
        }
        return Assert.notBlank(value, () -> new ArgumentException(error, CharSequenceUtil.format(message, args)));
    }


    public static <T extends CharSequence> T notEmpty(T value, ReturnCode error) {
        return notEmpty(value, error, null);
    }

    public static <T extends CharSequence> T notEmpty(T value, ReturnCode error, String message, Object... args) {
        if (!StringUtils.hasText(message)) {
            return Assert.notEmpty(value, () -> new ArgumentException(error));
        }
        return Assert.notEmpty(value, () -> new ArgumentException(error, CharSequenceUtil.format(message, args)));
    }


    public static <T> T[] notEmpty(T[] array, ReturnCode error) {
        return notEmpty(array, error, null);
    }

    public static <T> T[] notEmpty(T[] array, ReturnCode error, String message, Object... args) {
        if (!StringUtils.hasText(message)) {
            return Assert.notEmpty(array, () -> new ArgumentException(error));
        }
        return Assert.notEmpty(array, () -> new ArgumentException(error, CharSequenceUtil.format(message, args)));
    }


    public static <E, T extends Collection<E>> T notEmpty(T collection, ReturnCode error) {
        return notEmpty(collection, error, null);
    }

    public static <E, T extends Collection<E>> T notEmpty(T collection, ReturnCode error, String message, Object... args) {
        if (!StringUtils.hasText(message)) {
            return Assert.notEmpty(collection, () -> new ArgumentException(error));
        }
        return Assert.notEmpty(collection, () -> new ArgumentException(error, CharSequenceUtil.format(message, args)));
    }


    public static <K, V, T extends Map<K, V>> T notEmpty(T map, ReturnCode error) {
        return notEmpty(map, error, null);
    }

    public static <K, V, T extends Map<K, V>> T notEmpty(T map, ReturnCode error, String message, Object... args) {
        if (!StringUtils.hasText(message)) {
            return Assert.notEmpty(map, () -> new ArgumentException(error));
        }
        return Assert.notEmpty(map, () -> new ArgumentException(error, CharSequenceUtil.format(message, args)));
    }

    /**
     * 检验数据范围
     *
     * @param value
     * @param min
     * @param max
     * @return
     */

    public static int checkBetween(int value, int min, int max, ReturnCode error) {
        return Assert.checkBetween(value, min, max, () -> new ArgumentException(error));
    }

    public static int checkBetween(int value, int min, int max, ReturnCode error, String message, Object... args) {
        if (!StringUtils.hasText(message)) {
            return Assert.checkBetween(value, min, max, () -> new ArgumentException(error));
        }
        return Assert.checkBetween(value, min, max, () -> new ArgumentException(error, CharSequenceUtil.format(message, args)));
    }


    public static long checkBetween(long value, long min, long max, ReturnCode error) {
        return Assert.checkBetween(value, min, max, () -> new ArgumentException(error));
    }

    public static long checkBetween(long value, long min, long max, ReturnCode error, String message, Object... args) {
        if (!StringUtils.hasText(message)) {
            return Assert.checkBetween(value, min, max, () -> new ArgumentException(error));
        }
        return Assert.checkBetween(value, min, max, () -> new ArgumentException(error, CharSequenceUtil.format(message, args)));
    }


    public static double checkBetween(double value, double min, double max, ReturnCode error) {
        return Assert.checkBetween(value, min, max, () -> new ArgumentException(error));
    }

    public static double checkBetween(double value, double min, double max, ReturnCode error, String message, Object... args) {
        if (!StringUtils.hasText(message)) {
            return Assert.checkBetween(value, min, max, () -> new ArgumentException(error));
        }
        return Assert.checkBetween(value, min, max, () -> new ArgumentException(error, CharSequenceUtil.format(message, args)));
    }

    public static Number checkBetweenObj(Number value, Number min, Number max, ReturnCode error) {
        return checkBetweenObj(value, min, max, error, null);
    }

    public static Number checkBetweenObj(Number value, Number min, Number max, ReturnCode error, String message, Object... args) {
        try {
            return Assert.checkBetween(value, min, max);
        } catch (IllegalArgumentException ex) {
            if (!StringUtils.hasText(message)) {
                throw new ArgumentException(error);
            }
            throw new ArgumentException(error, CharSequenceUtil.format(message, args));
        }
    }

    /**
     * 是否相等
     *
     * @param obj1
     * @param obj2
     */
    public static void equals(Object obj1, Object obj2, ReturnCode error) {
        equals(obj1, obj2, error, null);
    }

    public static void equals(Object obj1, Object obj2, ReturnCode error, String message, Object... args) {
        if (!StringUtils.hasText(message)) {
            Assert.equals(obj1, obj2, () -> new ArgumentException(error));
            return;
        }
        Assert.equals(obj1, obj2, () -> new ArgumentException(error, CharSequenceUtil.format(message, args)));
    }


    public static void notEquals(Object obj1, Object obj2, ReturnCode error) {
        notEquals(obj1, obj2, error, null);
    }

    public static void notEquals(Object obj1, Object obj2, ReturnCode error, String message, Object... args) {
        if (!StringUtils.hasText(message)) {
            Assert.notEquals(obj1, obj2, () -> new ArgumentException(error));
            return;
        }
        Assert.notEquals(obj1, obj2, () -> new ArgumentException(error, CharSequenceUtil.format(message, args)));
    }


    public static void isTrue(boolean expression, ReturnCode error) {
        isTrue(expression, error, null);
    }

    public static void isTrue(boolean expression, ReturnCode error, String message, Object... args) {
        if (!StringUtils.hasText(message)) {
            Assert.isTrue(expression, () -> new ArgumentException(error));
            return;
        }
        Assert.isTrue(expression, () -> new ArgumentException(error, CharSequenceUtil.format(message, args)));
    }


    public static void isFalse(boolean expression, ReturnCode error) {
        isFalse(expression, error, null);
    }

    public static void isFalse(boolean expression, ReturnCode error, String message, Object... args) {
        if (!StringUtils.hasText(message)) {
            Assert.isFalse(expression, () -> new ArgumentException(error));
            return;
        }
        Assert.isFalse(expression, () -> new ArgumentException(error, CharSequenceUtil.format(message, args)));
    }

    public static <T> T notNull(T obj, ReturnCode error) {
        return notNull(obj, error, null);
    }

    public static <T> T notNull(T obj, ReturnCode error, String message, Object... args) {
        if (!StringUtils.hasText(message)) {
            return Assert.notNull(obj, () -> new ArgumentException(error));
        }
        return Assert.notNull(obj, () -> new ArgumentException(error, CharSequenceUtil.format(message, args)));
    }
}
