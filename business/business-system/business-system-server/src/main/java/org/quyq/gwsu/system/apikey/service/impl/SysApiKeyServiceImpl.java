package org.quyq.gwsu.system.apikey.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.api.vo.ApiKeyLoginUserVO;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.system.api.apikey.dto.ApiKeyCreateDTO;
import org.quyq.gwsu.system.api.apikey.dto.ApiKeyQueryDTO;
import org.quyq.gwsu.system.api.apikey.enums.ApiKeyExpireTypeEnum;
import org.quyq.gwsu.system.api.apikey.vo.ApiKeyCreateResultVO;
import org.quyq.gwsu.system.api.apikey.vo.ApiKeyDetailVO;
import org.quyq.gwsu.system.api.apikey.vo.ApiKeyVO;
import org.quyq.gwsu.system.apikey.domain.SysApiKey;
import org.quyq.gwsu.system.apikey.generator.ApiKeyHashManager;
import org.quyq.gwsu.system.apikey.generator.ApiKeyTokenGenerator;
import org.quyq.gwsu.system.apikey.mapper.SysApiKeyMapper;
import org.quyq.gwsu.system.apikey.service.ISysApiKeyService;
import org.quyq.gwsu.system.errcode.SystemErrorCode;
import org.quyq.gwsu.system.manager.service.ISysUserService;
import org.quyq.gwsu.system.api.manager.vo.SysUserDetailVO;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * API_KEY Service 实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = ISysApiKeyService.CACHE_API_KEY_PREFIX)
public class SysApiKeyServiceImpl extends ServiceImpl<SysApiKeyMapper, SysApiKey> implements ISysApiKeyService {

    private final SecurityUtils securityUtils;
    private final ApiKeyHashManager apiKeyHashManager;
    private final ApiKeyTokenGenerator apiKeyTokenGenerator;
    private final ISysUserService userService;
    private final CacheManager cacheManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiKeyCreateResultVO createCurrentUserApiKey(ApiKeyCreateDTO dto) {
        UserInfo currentUser = currentUser();
        validateCreateDTO(dto);
        LocalDateTime expireTime = resolveExpireTime(dto);

        String loginId = "USER:%s".formatted(currentUser.getUserId());
        String apiKey = apiKeyTokenGenerator.generateApiKey(loginId);
        String apiKeyHash = apiKeyHashManager.hash(apiKey);

        SysApiKey entity = new SysApiKey()
                .setUserId(currentUser.getUserId())
                .setApiKeyName(dto.getApiKeyName().trim())
                .setApiKeyHash(apiKeyHash)
                .setHashVersion(apiKeyHashManager.version())
                .setMaskedKey(apiKeyTokenGenerator.mask(apiKey))
                .setStatus(1)
                .setExpireTime(expireTime)
                .setRemark(dto.getRemark());
        save(entity);

        ApiKeyCreateResultVO vo = new ApiKeyCreateResultVO();
        vo.setId(entity.getId());
        vo.setApiKeyName(entity.getApiKeyName());
        vo.setApiKey(apiKey);
        vo.setExpireTime(expireTime);
        vo.setRemark(entity.getRemark());
        vo.copyBaseProperties(entity);
        return vo;
    }

    @Override
    public IPage<ApiKeyVO> pageCurrentUserApiKeys(ApiKeyQueryDTO query) {
        UserInfo currentUser = currentUser();
        Page<SysApiKey> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<SysApiKey> entityPage = baseMapper.selectPageByCondition(page, currentUser.getUserId(), query);
        Page<ApiKeyVO> result = Page.of(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream().map(SysApiKey::toVO).toList());
        return result;
    }

    @Override
    public ApiKeyDetailVO getCurrentUserApiKeyDetail(String id) {
        SysApiKey apiKey = getOwnedApiKey(id);
        return apiKey.toDetailVO();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeCurrentUserApiKey(String id) {
        SysApiKey apiKey = getOwnedApiKey(id);
        boolean updated = update(new LambdaUpdateWrapper<SysApiKey>()
                .eq(SysApiKey::getId, id)
                .eq(SysApiKey::getUserId, apiKey.getUserId())
                .eq(SysApiKey::getDeleted, false)
                .set(SysApiKey::getDeleted, true)
                .set(SysApiKey::getDeleteTime, LocalDateTime.now()));
        evictApiKeyCache(apiKey.getApiKeyHash());
        return updated;
    }

    @Override
    @Cacheable(key = "#root.target.buildApiKeyCacheKey(#apiKey)")
    public ApiKeyLoginUserVO validateApiKeyAndLoadUser(String apiKey) {
        AssertUtils.hasText(apiKey, CommonErrorCode.E03001);
        String apiKeyHash = apiKeyHashManager.hash(apiKey);
        SysApiKey entity = baseMapper.selectByApiKeyHash(apiKeyHash);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            throw new BusinessException(SystemErrorCode.E03006);
        }
        if (!Objects.equals(entity.getStatus(), 1)) {
            throw new BusinessException(SystemErrorCode.E03009);
        }
        if (entity.getExpireTime() != null && !entity.getExpireTime().isAfter(LocalDateTime.now())) {
            evictApiKeyCache(apiKeyHash);
            throw new BusinessException(SystemErrorCode.E03008);
        }

        SysUserDetailVO user = userService.getDetailById(entity.getUserId());
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(SystemErrorCode.E00004);
        }

        ApiKeyLoginUserVO vo = new ApiKeyLoginUserVO();
        vo.setUserId(user.getUserId());
        vo.setUserName(user.getUserName());
        vo.setStatus(user.getStatus());
        vo.setExpireTime(entity.getExpireTime());
        vo.setApiKeyId(entity.getId());
        vo.setLoginType("api_key");
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshApiKeyLastUsed(String apiKeyId, String requestIp) {
        baseMapper.updateLastUsedInfo(apiKeyId, LocalDateTime.now(), requestIp);
    }

    private void validateCreateDTO(ApiKeyCreateDTO dto) {
        AssertUtils.notNull(dto, SystemErrorCode.E03003);
        AssertUtils.hasText(dto.getApiKeyName(), SystemErrorCode.E03001);
        AssertUtils.isTrue(dto.getApiKeyName().trim().length() <= 128, SystemErrorCode.E03002);
        AssertUtils.notNull(dto.getExpireType(), SystemErrorCode.E03003);
        if (dto.getExpireType() == ApiKeyExpireTypeEnum.AFTER_DAYS) {
            AssertUtils.notNull(dto.getExpireDays(), SystemErrorCode.E03004);
            AssertUtils.isTrue(dto.getExpireDays() > 0, SystemErrorCode.E03004);
        }
        if (dto.getExpireType() == ApiKeyExpireTypeEnum.CUSTOM_DATE) {
            AssertUtils.notNull(dto.getExpireTime(), SystemErrorCode.E03005);
            AssertUtils.isTrue(dto.getExpireTime().isAfter(LocalDateTime.now()), SystemErrorCode.E03005);
        }
    }

    private LocalDateTime resolveExpireTime(ApiKeyCreateDTO dto) {
        if (dto.getExpireType() == ApiKeyExpireTypeEnum.FOREVER) {
            return null;
        }
        if (dto.getExpireType() == ApiKeyExpireTypeEnum.AFTER_DAYS) {
            return LocalDateTime.now().plusDays(dto.getExpireDays());
        }
        return dto.getExpireTime();
    }

    private SysApiKey getOwnedApiKey(String id) {
        UserInfo currentUser = currentUser();
        SysApiKey apiKey = getById(id);
        if (apiKey == null || Boolean.TRUE.equals(apiKey.getDeleted())) {
            throw new BusinessException(SystemErrorCode.E03006);
        }
        if (!Objects.equals(currentUser.getUserId(), apiKey.getUserId())) {
            throw new BusinessException(SystemErrorCode.E03007);
        }
        return apiKey;
    }

    private UserInfo currentUser() {
        return securityUtils.userInfo()
                .orElseThrow(() -> new BusinessException(CommonErrorCode.E03001));
    }

    public String buildApiKeyCacheKey(String apiKey) {
        return apiKeyHashManager.hash(apiKey);
    }

    private void evictApiKeyCache(String apiKeyHash) {
        Cache cache = cacheManager.getCache(CACHE_API_KEY_PREFIX);
        if (cache != null && StringUtils.hasText(apiKeyHash)) {
            cache.evict(apiKeyHash);
        }
    }
}
