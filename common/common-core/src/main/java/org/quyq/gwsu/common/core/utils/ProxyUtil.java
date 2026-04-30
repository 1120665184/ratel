package org.quyq.gwsu.common.core.utils;

import org.springframework.util.ClassUtils;

public class ProxyUtil {

    private ProxyUtil() {
    }


    public static boolean hasClass(String classname) {

        try {
            Class.forName(classname, false, ClassUtils.getDefaultClassLoader());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
