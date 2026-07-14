package org.quyq.gwsu.system.apikey.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.system.api.apikey.dto.ApiKeyQueryDTO;
import org.quyq.gwsu.system.apikey.domain.SysApiKey;

import java.time.LocalDateTime;

/**
 * API_KEY Mapper
 *
 * @author Quyq
 */
public interface SysApiKeyMapper extends BaseMapper<SysApiKey> {

    IPage<SysApiKey> selectPageByCondition(IPage<SysApiKey> page,
                                          @Param("userId") String userId,
                                          @Param("query") ApiKeyQueryDTO query);

    SysApiKey selectByApiKeyHash(@Param("apiKeyHash") String apiKeyHash);

    int updateLastUsedInfo(@Param("id") String id,
                           @Param("lastUsedTime") LocalDateTime lastUsedTime,
                           @Param("lastUsedIp") String lastUsedIp);
}
