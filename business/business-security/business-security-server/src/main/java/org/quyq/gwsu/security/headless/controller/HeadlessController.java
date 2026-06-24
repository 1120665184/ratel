package org.quyq.gwsu.security.headless.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.annotation.LoginAllowAccess;
import org.quyq.gwsu.security.api.config.dto.ConfigSaveDTO;
import org.quyq.gwsu.security.api.config.enums.ConfigValueType;
import org.quyq.gwsu.common.security.api.vo.ConfigVO;
import org.quyq.gwsu.security.dict.service.ISecurityConfigService;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.headless.domain.EntranceConfig;
import org.quyq.gwsu.security.headless.entrance.DingTalkEntrance;
import org.quyq.gwsu.security.headless.entrance.dingtalk.DingTalkClient;
import org.quyq.gwsu.security.headless.enums.EntranceType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Quyq
 * @date 2026/6/23
 * @description 无头控制智能体模块
 */
@RestController
@RequestMapping("headless")
@Tag(name = "无头控制智能体模块")
@RequiredArgsConstructor
public class HeadlessController {

    private final ISecurityConfigService configService;

    private final DingTalkEntrance entrance;

    private final ObjectMapper objectMapper;


    @Operation(summary = "保存远程操作配置")
    @PostMapping("config/remote-control")
    public R<Boolean> saveRemoteControlConfig(@RequestBody EntranceConfig config) {
        AssertUtils.notNull(config.getType(), SecurityErrorCode.E07012);

        if (EntranceType.DING_TALK == config.getType()) {
            AssertUtils.hasText(config.getDingTalk().getClientId(), SecurityErrorCode.E07007);
            AssertUtils.hasText(config.getDingTalk().getClientSecret(), SecurityErrorCode.E07008);
            AssertUtils.hasText(config.getDingTalk().getAiCardTemplateId(), SecurityErrorCode.E07011);
            AssertUtils.hasText(config.getDingTalk().getProtocol(), SecurityErrorCode.E07012);
            AssertUtils.hasText(config.getDingTalk().getRegionId(), SecurityErrorCode.E07012);
            AssertUtils.hasText(config.getDingTalk().getEndpoint(), SecurityErrorCode.E07012);
        }

        ConfigSaveDTO dto = new ConfigSaveDTO();
        // 查询已有配置，实现新增/更新判断
        ConfigVO existing = configService.getByKey(DingTalkClient.ASSISTANT_REMOTE_CONTROL_CONFIG_KEY);
        if (existing != null) {
            dto.setId(existing.getId());
        }
        dto.setConfigKey(DingTalkClient.ASSISTANT_REMOTE_CONTROL_CONFIG_KEY);
        dto.setConfigName("助手远程操作配置");
        dto.setConfigValue(toJson(config));
        dto.setValueType(ConfigValueType.JSON);
        dto.setDescription("AI 助手远程操作配置（钉钉等）");
        configService.saveOrUpdateConfig(dto);

        try {
            entrance.init();
        } catch (Exception e) {
            return R.fail("配置已保存，但初始化远程操作失败：" + e.getMessage());
        }

        return R.ok(true);
    }

    private String toJson(EntranceConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new BusinessException("配置序列化失败");
        }
    }

}
