package org.quyq.gwsu.system.apikey.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.system.api.apikey.vo.ApiKeyDetailVO;
import org.quyq.gwsu.system.api.apikey.vo.ApiKeyVO;

import java.time.LocalDateTime;

/**
 * API_KEY 实体
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "sys_api_key", autoResultMap = true)
@Schema(description = "API_KEY 实体")
public class SysApiKey extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "名称")
    private String apiKeyName;

    @Schema(description = "API_KEY 摘要")
    private String apiKeyHash;

    @Schema(description = "摘要版本")
    private Integer hashVersion;

    @Schema(description = "脱敏后的 Key")
    private String maskedKey;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "最近使用时间")
    private LocalDateTime lastUsedTime;

    @Schema(description = "最近使用IP")
    private String lastUsedIp;

    @Schema(description = "备注")
    private String remark;

    public ApiKeyVO toVO() {
        ApiKeyVO vo = new ApiKeyVO();
        vo.setId(id);
        vo.setApiKeyName(apiKeyName);
        vo.setMaskedKey(maskedKey);
        vo.setStatus(status);
        vo.setExpireTime(expireTime);
        vo.setLastUsedTime(lastUsedTime);
        vo.setRemark(remark);
        vo.copyBaseProperties(this);
        return vo;
    }

    public ApiKeyDetailVO toDetailVO() {
        ApiKeyDetailVO vo = new ApiKeyDetailVO();
        vo.setId(id);
        vo.setApiKeyName(apiKeyName);
        vo.setMaskedKey(maskedKey);
        vo.setStatus(status);
        vo.setExpireTime(expireTime);
        vo.setLastUsedTime(lastUsedTime);
        vo.setLastUsedIp(lastUsedIp);
        vo.setRemark(remark);
        vo.copyBaseProperties(this);
        return vo;
    }
}
