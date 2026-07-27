package org.quyq.gwsu.common.ai.agui.push;


import org.quyq.gwsu.common.ai.agui.event.AguiEvent;
import org.quyq.gwsu.common.ai.agui.domain.RunAgentInput;

/**
 * @author Quyq
 * @date 2026/6/15
 * @description 将agui消息推送到其他媒介供其他业务消费
 */
public interface AguiEventPusher {

    /**
     * 推送
     * @param param
     * @param event
     */
    void push(RunAgentInput param , AguiEvent event);


}
