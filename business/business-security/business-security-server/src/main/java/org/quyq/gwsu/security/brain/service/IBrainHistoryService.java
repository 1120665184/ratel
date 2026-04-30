package org.quyq.gwsu.security.brain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.agentscope.core.agui.model.AguiMessage;
import org.quyq.gwsu.security.api.brain.dto.BrainHistoryQueryDTO;
import org.quyq.gwsu.security.api.brain.vo.BrainHistorySessionVo;

import java.util.List;

/**
 * 大脑历史会话服务接口
 *
 * @author Quyq
 */
public interface IBrainHistoryService {

    /**
     * 分页查询历史会话列表
     *
     * @param query  查询条件
     * @param userId 用户ID
     * @return 分页结果
     */
    IPage<BrainHistorySessionVo> pageHistorySessions(BrainHistoryQueryDTO query, String userId);

    /**
     * 查询会话的消息列表
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 消息列表
     */
    List<AguiMessage> getSessionMessages(String sessionId, String userId);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 是否成功
     */
    Boolean deleteSession(String sessionId, String userId);
}
