package org.quyq.gwsu.common.ai.utils;


import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;

import java.util.List;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/5/13
 * @description llm消息处理工具类
 */
public class AIMsgUtils {

    private AIMsgUtils() {}


    /**
     * 获取系统消息
     * @param msgs
     * @return
     */
    public static Msg getSystemMsg(List<Msg> msgs) {
        if(Objects.isNull(msgs)){
            return null ;
        }

        return msgs.stream().filter(v -> MsgRole.SYSTEM == v.getRole())
                .findFirst().orElse(null);

    }

    /**
     * 替换系统消息
     * @param msgs
     * @param sysMsg
     */
    public static void replaceSystemMsg(List<Msg> msgs , Msg sysMsg) {
        if(Objects.isNull(msgs)){
            return;
        }
        msgs.removeIf(v -> MsgRole.SYSTEM == v.getRole());
        msgs.addFirst(sysMsg);
    }

    /**
     * 获取消息列表中最后一条用户消息
     * @param msgs
     * @return
     */
    public static Msg getLastUserMsg(List<Msg> msgs) {
        if(Objects.isNull(msgs)){
            return null ;
        }
        for (int i = msgs.size() - 1; i >= 0; i--) {
            Msg msg = msgs.get(i);
            if (MsgRole.USER == msg.getRole()) {
                return msg;
            }
        }

        return null;
    }

}
