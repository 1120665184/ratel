package org.quyq.gwsu.security.brain.service;


import org.quyq.gwsu.common.ai.agui.processor.AguiRequestProcessor;

/**
 * @author Quyq
 * @date 2026/4/22
 * @description
 */
public interface IBrainService {

    String AGENT_ID = "brain";


    AguiRequestProcessor buildAguiProcessor();

}
