package org.quyq.gwsu.common.core.domain;


/**
 * @author Quyq
 * @date 2026/3/10
 * @description
 */
public interface ReturnCode {

    /**
     * 错误码代表的含义
     * @return
     */
    String msg();


    default String code() {
        if(this instanceof Enum<?> tmp){
            return tmp.name();
        }

        throw new IllegalArgumentException("返回码必须是枚举类型");
    }

}
