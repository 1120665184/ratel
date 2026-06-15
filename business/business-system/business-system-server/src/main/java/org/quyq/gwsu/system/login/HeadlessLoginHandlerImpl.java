package org.quyq.gwsu.system.login;


import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.authentication.exception.AuthException;
import org.quyq.gwsu.common.authentication.login.impl.headless.HeadlessLoginHandler;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.system.api.manager.vo.SysUserDetailVO;
import org.quyq.gwsu.system.errcode.SystemErrorCode;
import org.quyq.gwsu.system.manager.service.ISysUserService;
import org.springframework.stereotype.Component;

/**
 * @author Quyq
 * @date 2026/6/13
 * @description
 */
@Slf4j
@Component
public class HeadlessLoginHandlerImpl extends HeadlessLoginHandler<SysUserDetailVO> {
    public HeadlessLoginHandlerImpl(CacheUtils cacheUtils) {
        super(cacheUtils);
    }

    @Resource
    private ISysUserService userService;

    @Override
    protected SysUserDetailVO getUserInfo(String userId) {
        log.info("[HeadlessLogin] 从缓存中获取到userId: {}", userId);

        // 4. 查询用户
        SysUserDetailVO user = userService.getDetailById(userId);
        if (user == null) {
            throw new AuthException(userId, SystemErrorCode.E00003);
        }

        // 5. 检查用户状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new AuthException(user.getUserName(), SystemErrorCode.E00004);
        }

        return user;
    }
}
