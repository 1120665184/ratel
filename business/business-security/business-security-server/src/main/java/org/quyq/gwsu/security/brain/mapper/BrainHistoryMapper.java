package org.quyq.gwsu.security.brain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.security.api.brain.vo.BrainHistorySessionVo;

import java.util.List;

/**
 * 大脑历史会话 Mapper 接口
 *
 * @author Quyq
 */
@Mapper
public interface BrainHistoryMapper {

    /**
     * 查询用户的历史会话列表
     *
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 历史会话列表
     */
    List<BrainHistorySessionVo> selectHistorySessions(
            @Param("userId") String userId,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit);

    /**
     * 查询用户的历史会话总数
     *
     * @param userId 用户ID
     * @return 总数
     */
    Long countHistorySessions(@Param("userId") String userId);

    /**
     * 查询会话的消息列表
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 消息列表
     */
    List<String> selectSessionMessages(
            @Param("sessionId") String sessionId,
            @Param("userId") String userId);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 删除数量
     */
    int deleteSession(
            @Param("sessionId") String sessionId,
            @Param("userId") String userId);
}
