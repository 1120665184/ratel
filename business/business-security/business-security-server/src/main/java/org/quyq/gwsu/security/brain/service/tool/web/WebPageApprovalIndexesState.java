package org.quyq.gwsu.security.brain.service.tool.web;

import io.agentscope.core.state.State;

import java.util.List;

/**
 * 持久化保存当前会话最近一次页面快照中的审批按钮索引。
 */
public record WebPageApprovalIndexesState(List<Integer> indexes) implements State {
}
