package org.quyq.gwsu.security.brain.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import io.agentscope.core.agui.model.AguiMessage;
import io.agentscope.core.session.Session;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.AguiController;
import org.quyq.gwsu.common.ai.agui.domain.CopilotKitInfo;
import org.quyq.gwsu.common.ai.agui.dto.ChatDTO;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.agui.web.WebToolCallbackRequest;
import org.quyq.gwsu.common.ai.loop.domain.HumanApprovalInfo;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.security.annotation.LoginAllowAccess;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.security.api.brain.dto.BrainHistoryQueryDTO;
import org.quyq.gwsu.security.api.brain.vo.BrainHistorySessionVo;
import org.quyq.gwsu.security.brain.push.AguiEventRedisPusher;
import org.quyq.gwsu.security.brain.service.IBrainHistoryService;
import org.quyq.gwsu.security.brain.service.IBrainService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * CopilotKit Runtime 端点控制器
 *
 * @author Quyq
 * @date 2026/4/22
 */
@LoginAllowAccess
@RestController
@RequestMapping("brain")
@Tag(name = "智能助手模块")
@Slf4j
public class BrainController implements DisposableBean {

    private static final String DEFAULT_AGENT_ID_HEADER = "X-Agent-Id";

    private final AguiController aguiController;
    private final IBrainHistoryService brainHistoryService;
    private final SecurityUtils securityUtils;


    public BrainController(IBrainService brainService , CacheUtils cacheUtils , ObjectMapper mapper, Session agentSession, SecurityUtils securityUtils,
                           IBrainHistoryService brainHistoryService, WebToolUtils webToolUtils) {
        this.brainHistoryService = brainHistoryService;
        this.securityUtils = securityUtils;
        this.aguiController = new AguiController(brainService.buildAguiProcessor(), webToolUtils, securityUtils,600000L) {
            @Override
            protected CopilotKitInfo handleInfo() {
                return new CopilotKitInfo()
                        .addAgent(new CopilotKitInfo.Agents(IBrainService.AGENT_ID, "平台中央大脑"));
            }
        };

        this.aguiController.setAgentSession(agentSession);
        this.aguiController.addPusher(new AguiEventRedisPusher(cacheUtils , mapper));

    }


    /**
     * 中央大脑统一入口
     */
    @Operation(summary = "中央大脑（智能助手）聊天入口")
    @PostMapping(value = "run/copilotKit",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object handleCopilotKitRequest(@RequestBody ChatDTO request,
                                          @RequestHeader(value = DEFAULT_AGENT_ID_HEADER, required = false) String headerAgentId) {

        return aguiController.handleCopilotKitRequest(request, headerAgentId);
    }

    @Operation(summary = "分页查询历史会话列表")
    @PostMapping("history/sessions")
    public R<IPage<BrainHistorySessionVo>> pageHistorySessions(@RequestBody BrainHistoryQueryDTO query) {
        String userId = securityUtils.userInfo().map(UserInfo::getUserId).orElse(null);
        if (userId == null) {
            return R.fail("用户未登录");
        }
        return R.ok(brainHistoryService.pageHistorySessions(query, userId));
    }

    @Operation(summary = "查询会话消息列表")
    @GetMapping("history/sessions/{sessionId}/messages")
    public R<List<AguiMessage>> getSessionMessages(@PathVariable String sessionId) {
        String userId = securityUtils.userInfo().map(UserInfo::getUserId).orElse(null);
        if (userId == null) {
            return R.fail("用户未登录");
        }
        return R.ok(brainHistoryService.getSessionMessages(sessionId, userId));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("history/sessions/{sessionId}")
    public R<Boolean> deleteSession(@PathVariable String sessionId) {
        String userId = securityUtils.userInfo().map(UserInfo::getUserId).orElse(null);
        if (userId == null) {
            return R.fail("用户未登录");
        }
        return R.ok(brainHistoryService.deleteSession(sessionId, userId));
    }

    @Operation(summary = "前端工具执行结果回调")
    @PostMapping("tool/callback")
    public R<Void> toolCallback(@RequestBody WebToolCallbackRequest request) {
        return aguiController.handleToolCallback(request);
    }

    @Operation(summary = "查询会话审批状态")
    @GetMapping("approval/status/{threadId}")
    public R<HumanApprovalInfo> getApprovalStatus(@PathVariable String threadId) {
        return aguiController.handleApprovalStatus(threadId);
    }

    @Override
    public void destroy() throws Exception {
        aguiController.destroy();
    }
}
