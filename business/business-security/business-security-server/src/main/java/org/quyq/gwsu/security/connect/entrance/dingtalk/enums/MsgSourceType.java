package org.quyq.gwsu.security.connect.entrance.dingtalk.enums;


import org.quyq.gwsu.common.core.exception.BusinessException;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description
 */
public enum MsgSourceType {

    PRIVATE_CHAT ,
    GROUP ;

    public static MsgSourceType getMsgSourceType(String type){
        return getMsgSourceType(Integer.parseInt(type));
    }

    public static MsgSourceType getMsgSourceType(int type) {
        if(type == 1){
            return PRIVATE_CHAT;
        }else if (type == 2){
            return GROUP;
        }
        throw new BusinessException("未知的消息类型");
    }

}
