package org.quyq.gwsu.common.authentication.login.logic;


import cn.dev33.satoken.config.SaCookieConfig;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.stp.parameter.SaLogoutParameter;
import cn.dev33.satoken.stp.parameter.enums.SaLogoutMode;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Quyq
 * @date 2026/4/9
 * @description 代理Logic , 不实现任何逻辑，只做对不同账号登录处理实现的代理
 */
public class ProxyLogic extends StpLogic {

    public ProxyLogic() {
        super(null);
    }

    /**
     * 获取当前 StpLogic 账号类型标识
     *
     * @return /
     */
    public String getLoginType() {
        return LogicUtils.getLogic().map(StpLogic::getLoginType).orElse(null);
    }

    /**
     * 安全的重置当前账号类型
     *
     * @param loginType 账号类型标识
     * @return 对象自身
     */
    public StpLogic setLoginType(String loginType) {
        return this;
    }

    /**
     * 写入当前 StpLogic 单独使用的配置对象
     *
     * @param config 配置对象
     * @return 对象自身
     */
    public StpLogic setConfig(SaTokenConfig config) {
        LogicUtils.getLogic().ifPresent(logic -> logic.setConfig(config));
        return this;
    }

    /**
     * 返回当前 StpLogic 使用的配置对象，如果当前 StpLogic 没有配置，则返回 null
     *
     * @return /
     */
    public SaTokenConfig getConfig() {
        return LogicUtils.getLogic().map(StpLogic::getConfig).orElse(null);
    }

    /**
     * 返回当前 StpLogic 使用的配置对象，如果当前 StpLogic 没有配置，则返回全局配置对象
     *
     * @return /
     */
    public SaTokenConfig getConfigOrGlobal() {
        return LogicUtils.getLogic().map(StpLogic::getConfigOrGlobal).orElse(null);
    }


    // ------------------- 获取 token 相关 -------------------

    /**
     * 返回 token 名称，此名称在以下地方体现：Cookie 保存 token 时的名称、提交 token 时参数的名称、存储 token 时的 key 前缀
     *
     * @return /
     */
    public String getTokenName() {
        return LogicUtils.getLogic().map(StpLogic::getTokenName).orElse(null);
    }

    /**
     * 为指定账号创建一个 token （只是把 token 创建出来，并不持久化存储）
     *
     * @param loginId    账号id
     * @param deviceType 设备类型
     * @param timeout    过期时间
     * @param extraData  扩展信息
     * @return 生成的tokenValue
     */
    public String createTokenValue(Object loginId, String deviceType, long timeout, Map<String, Object> extraData) {
        return LogicUtils.getLogic().map(logic -> logic.createTokenValue(loginId, deviceType, timeout, extraData)).orElse(null);
    }

    /**
     * 在当前会话写入指定 token 值
     *
     * @param tokenValue token 值
     */
    public void setTokenValue(String tokenValue) {
        LogicUtils.getLogic().ifPresent(logic -> logic.setTokenValue(tokenValue));
    }

    /**
     * 在当前会话写入指定 token 值
     *
     * @param tokenValue    token 值
     * @param cookieTimeout Cookie存活时间(秒)
     */
    public void setTokenValue(String tokenValue, int cookieTimeout) {
        LogicUtils.getLogic().ifPresent(logic -> logic.setTokenValue(tokenValue, cookieTimeout));
    }

    /**
     * 在当前会话写入指定 token 值
     *
     * @param tokenValue     token 值
     * @param loginParameter 登录参数
     */
    public void setTokenValue(String tokenValue, SaLoginParameter loginParameter) {
        LogicUtils.getLogic().ifPresent(logic -> logic.setTokenValue(tokenValue, loginParameter));
    }

    /**
     * 将 token 写入到当前请求的 Storage 存储器里
     *
     * @param tokenValue 要保存的 token 值
     */
    public void setTokenValueToStorage(String tokenValue) {
        LogicUtils.getLogic().ifPresent(logic -> logic.setTokenValueToStorage(tokenValue));
    }

    /**
     * 将 token 写入到当前会话的 Cookie 里
     *
     * @param tokenValue    token 值
     * @param cookieTimeout Cookie存活时间（单位：秒，填-1代表为内存Cookie，浏览器关闭后消失）
     */
    public void setTokenValueToCookie(String tokenValue, int cookieTimeout) {
        LogicUtils.getLogic().ifPresent(logic -> logic.setTokenValueToCookie(tokenValue, cookieTimeout));
    }

    /**
     * 将 token 写入到当前会话的 Cookie 里
     *
     * @param tokenValue    token 值
     * @param cookieConfig  Cookie 配置项
     * @param cookieTimeout Cookie存活时间（单位：秒，填-1代表为内存Cookie，浏览器关闭后消失）
     */
    public void setTokenValueToCookie(String tokenValue, SaCookieConfig cookieConfig, int cookieTimeout) {
        LogicUtils.getLogic().ifPresent(logic -> logic.setTokenValueToCookie(tokenValue, cookieConfig, cookieTimeout));
    }

    /**
     * 将 token 写入到当前请求的响应头中
     *
     * @param tokenValue token 值
     */
    public void setTokenValueToResponseHeader(String tokenValue) {
        LogicUtils.getLogic().ifPresent(logic -> logic.setTokenValueToResponseHeader(tokenValue));
    }

    /**
     * 获取当前请求的 token 值
     *
     * @return 当前tokenValue
     */
    public String getTokenValue() {
        return LogicUtils.getLogic().map(StpLogic::getTokenValue).orElse(null);
    }

    /**
     * 获取当前请求的 token 值
     *
     * @param noPrefixThrowException 如果提交的 token 不带有指定的前缀，是否抛出异常
     * @return 当前tokenValue
     */
    public String getTokenValue(boolean noPrefixThrowException) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenValue(noPrefixThrowException)).orElse(null);
    }

    /**
     * 获取当前请求的 token 值 （不裁剪前缀）
     *
     * @return /
     */
    public String getTokenValueNotCut() {
        return LogicUtils.getLogic().map(StpLogic::getTokenValueNotCut).orElse(null);
    }

    /**
     * 获取当前请求的 token 值，如果获取不到则抛出异常
     *
     * @return /
     */
    public String getTokenValueNotNull() {
        return LogicUtils.getLogic().map(StpLogic::getTokenValueNotNull).orElse(null);
    }

    /**
     * 获取当前会话的 token 参数信息
     *
     * @return token 参数信息
     */
    public SaTokenInfo getTokenInfo() {
        return LogicUtils.getLogic().map(StpLogic::getTokenInfo).orElse(null);
    }


    // ------------------- 登录相关操作 -------------------

    // --- 登录

    /**
     * 会话登录
     *
     * @param id 账号id，建议的类型：（long | int | String）
     */
    public void login(Object id) {
        LogicUtils.getLogic().ifPresent(logic -> logic.login(id));
    }

    /**
     * 会话登录，并指定登录设备类型
     *
     * @param id         账号id，建议的类型：（long | int | String）
     * @param deviceType 设备类型
     */
    public void login(Object id, String deviceType) {
        LogicUtils.getLogic().ifPresent(logic -> logic.login(id, deviceType));
    }

    /**
     * 会话登录，并指定是否 [记住我]
     *
     * @param id              账号id，建议的类型：（long | int | String）
     * @param isLastingCookie 是否为持久Cookie，值为 true 时记住我，值为 false 时关闭浏览器需要重新登录
     */
    public void login(Object id, boolean isLastingCookie) {
        LogicUtils.getLogic().ifPresent(logic -> logic.login(id, isLastingCookie));
    }

    /**
     * 会话登录，并指定此次登录 token 的有效期, 单位:秒
     *
     * @param id      账号id，建议的类型：（long | int | String）
     * @param timeout 此次登录 token 的有效期, 单位:秒
     */
    public void login(Object id, long timeout) {
        LogicUtils.getLogic().ifPresent(logic -> logic.login(id, timeout));
    }

    /**
     * 会话登录，并指定所有登录参数 Model
     *
     * @param id             账号id，建议的类型：（long | int | String）
     * @param loginParameter 此次登录的参数Model
     */
    public void login(Object id, SaLoginParameter loginParameter) {
        LogicUtils.getLogic().ifPresent(logic -> logic.login(id, loginParameter));
    }

    /**
     * 创建指定账号 id 的登录会话数据
     *
     * @param id 账号id，建议的类型：（long | int | String）
     * @return 返回会话令牌
     */
    public String createLoginSession(Object id) {
        return LogicUtils.getLogic().map(logic -> logic.createLoginSession(id)).orElse(null);
    }

    /**
     * 创建指定账号 id 的登录会话数据
     *
     * @param id             账号id，建议的类型：（long | int | String）
     * @param loginParameter 此次登录的参数Model
     * @return 返回会话令牌
     */
    public String createLoginSession(Object id, SaLoginParameter loginParameter) {
        return LogicUtils.getLogic().map(logic -> logic.createLoginSession(id, loginParameter)).orElse(null);
    }


    /**
     * 获取指定账号 id 的登录会话数据，如果获取不到则创建并返回
     *
     * @param id 账号id，建议的类型：（long | int | String）
     * @return 返回会话令牌
     */
    public String getOrCreateLoginSession(Object id) {
        return LogicUtils.getLogic().map(logic -> logic.getOrCreateLoginSession(id)).orElse(null);
    }

    // --- 注销 (根据 token)

    /**
     * 在当前客户端会话注销
     */
    public void logout() {
        LogicUtils.getLogic().ifPresent(StpLogic::logout);
    }

    /**
     * 在当前客户端会话注销，根据注销参数
     */
    public void logout(SaLogoutParameter logoutParameter) {
        LogicUtils.getLogic().ifPresent(logic -> logic.logout(logoutParameter));
    }

    /**
     * 注销下线，根据指定 token
     *
     * @param tokenValue 指定 token
     */
    public void logoutByTokenValue(String tokenValue) {
        LogicUtils.getLogic().ifPresent(logic -> logic.logoutByTokenValue(tokenValue));
    }

    /**
     * 注销下线，根据指定 token、注销参数
     *
     * @param tokenValue      指定 token
     * @param logoutParameter /
     */
    public void logoutByTokenValue(String tokenValue, SaLogoutParameter logoutParameter) {
        LogicUtils.getLogic().ifPresent(logic -> logic.logoutByTokenValue(tokenValue, logoutParameter));
    }

    /**
     * 踢人下线，根据指定 token
     * <p> 当对方再次访问系统时，会抛出 NotLoginException 异常，场景值=-5 </p>
     *
     * @param tokenValue 指定 token
     */
    public void kickoutByTokenValue(String tokenValue) {
        LogicUtils.getLogic().ifPresent(logic -> logic.kickoutByTokenValue(tokenValue));
    }

    /**
     * 踢人下线，根据指定 token、注销参数
     * <p> 当对方再次访问系统时，会抛出 NotLoginException 异常，场景值=-5 </p>
     *
     * @param tokenValue      指定 token
     * @param logoutParameter 注销参数
     */
    public void kickoutByTokenValue(String tokenValue, SaLogoutParameter logoutParameter) {
        LogicUtils.getLogic().ifPresent(logic -> logic.kickoutByTokenValue(tokenValue, logoutParameter));
    }

    /**
     * 顶人下线，根据指定 token
     * <p> 当对方再次访问系统时，会抛出 NotLoginException 异常，场景值=-4 </p>
     *
     * @param tokenValue 指定 token
     */
    public void replacedByTokenValue(String tokenValue) {
        LogicUtils.getLogic().ifPresent(logic -> logic.replacedByTokenValue(tokenValue));
    }

    /**
     * 顶人下线，根据指定 token、注销参数
     * <p> 当对方再次访问系统时，会抛出 NotLoginException 异常，场景值=-4 </p>
     *
     * @param tokenValue      指定 token
     * @param logoutParameter /
     */
    public void replacedByTokenValue(String tokenValue, SaLogoutParameter logoutParameter) {
        LogicUtils.getLogic().ifPresent(logic -> logic.replacedByTokenValue(tokenValue, logoutParameter));
    }

    /**
     * [work] 注销下线，根据指定 token 、注销参数
     *
     * @param tokenValue      指定 token
     * @param logoutParameter 注销参数
     */
    public void _logoutByTokenValue(String tokenValue, SaLogoutParameter logoutParameter) {
        LogicUtils.getLogic().ifPresent(logic -> logic._logoutByTokenValue(tokenValue, logoutParameter));
    }

    // --- 注销 (根据 loginId)

    /**
     * 会话注销，根据账号id
     *
     * @param loginId 账号id
     */
    public void logout(Object loginId) {
        LogicUtils.getLogic().ifPresent(logic -> logic.logout(loginId));
    }

    /**
     * 会话注销，根据账号id 和 设备类型
     *
     * @param loginId    账号id
     * @param deviceType 设备类型 (填 null 代表注销该账号的所有设备类型)
     */
    public void logout(Object loginId, String deviceType) {
        LogicUtils.getLogic().ifPresent(logic -> logic.logout(loginId, deviceType));
    }

    /**
     * 会话注销，根据账号id 和 注销参数
     *
     * @param loginId         账号id
     * @param logoutParameter 注销参数
     */
    public void logout(Object loginId, SaLogoutParameter logoutParameter) {
        LogicUtils.getLogic().ifPresent(logic -> logic.logout(loginId, logoutParameter));
    }

    /**
     * 踢人下线，根据账号id
     * <p> 当对方再次访问系统时，会抛出 NotLoginException 异常，场景值=-5 </p>
     *
     * @param loginId 账号id
     */
    public void kickout(Object loginId) {
        LogicUtils.getLogic().ifPresent(logic -> logic.kickout(loginId));
    }

    /**
     * 踢人下线，根据账号id 和 设备类型
     * <p> 当对方再次访问系统时，会抛出 NotLoginException 异常，场景值=-5 </p>
     *
     * @param loginId    账号id
     * @param deviceType 设备类型 (填 null 代表踢出该账号的所有设备类型)
     */
    public void kickout(Object loginId, String deviceType) {
        LogicUtils.getLogic().ifPresent(logic -> logic.kickout(loginId, deviceType));
    }

    /**
     * 踢人下线，根据账号id 和 注销参数
     * <p> 当对方再次访问系统时，会抛出 NotLoginException 异常，场景值=-5 </p>
     *
     * @param loginId         账号id
     * @param logoutParameter 注销参数
     */
    public void kickout(Object loginId, SaLogoutParameter logoutParameter) {
        LogicUtils.getLogic().ifPresent(logic -> logic.kickout(loginId, logoutParameter));
    }

    /**
     * 顶人下线，根据账号id
     * <p> 当对方再次访问系统时，会抛出 NotLoginException 异常，场景值=-4 </p>
     *
     * @param loginId 账号id
     */
    public void replaced(Object loginId) {
        LogicUtils.getLogic().ifPresent(logic -> logic.replaced(loginId));
    }

    /**
     * 顶人下线，根据账号id 和 设备类型
     * <p> 当对方再次访问系统时，会抛出 NotLoginException 异常，场景值=-4 </p>
     *
     * @param loginId    账号id
     * @param deviceType 设备类型 （填 null 代表顶替该账号的所有设备类型）
     */
    public void replaced(Object loginId, String deviceType) {
        LogicUtils.getLogic().ifPresent(logic -> logic.replaced(loginId, deviceType));
    }

    /**
     * 顶人下线，根据账号id 和 注销参数
     * <p> 当对方再次访问系统时，会抛出 NotLoginException 异常，场景值=-4 </p>
     *
     * @param loginId         账号id
     * @param logoutParameter 注销参数
     */
    public void replaced(Object loginId, SaLogoutParameter logoutParameter) {
        LogicUtils.getLogic().ifPresent(logic -> logic.replaced(loginId, logoutParameter));
    }

    /**
     * [work] 会话注销，根据账号id 和 注销参数
     *
     * @param loginId         账号id
     * @param logoutParameter 注销参数
     */
    public void _logout(Object loginId, SaLogoutParameter logoutParameter) {
        LogicUtils.getLogic().ifPresent(logic -> logic._logout(loginId, logoutParameter));
    }

    // --- 注销 (会话管理辅助方法)

    /**
     * 在 Account-Session 上移除 Terminal 信息 (注销下线方式)
     *
     * @param session  /
     * @param terminal /
     */
    public void removeTerminalByLogout(SaSession session, SaTerminalInfo terminal) {
        LogicUtils.getLogic().ifPresent(logic -> logic.removeTerminalByLogout(session, terminal));
    }

    /**
     * 在 Account-Session 上移除 Terminal 信息 (踢人下线方式)
     *
     * @param session  /
     * @param terminal /
     */
    public void removeTerminalByKickout(SaSession session, SaTerminalInfo terminal) {
        LogicUtils.getLogic().ifPresent(logic -> logic.removeTerminalByKickout(session, terminal));
    }

    /**
     * 在 Account-Session 上移除 Terminal 信息 (顶人下线方式)
     *
     * @param session  /
     * @param terminal /
     */
    public void removeTerminalByReplaced(SaSession session, SaTerminalInfo terminal) {
        LogicUtils.getLogic().ifPresent(logic -> logic.removeTerminalByReplaced(session, terminal));
    }

    /**
     * 在 Account-Session 上移除 Terminal 信息 (内部方法，仅为减少重复代码，外部调用意义不大)
     *
     * @param session         Account-Session
     * @param terminal        设备信息
     * @param logoutParameter 注销参数
     */
    public void _removeTerminal(SaSession session, SaTerminalInfo terminal, SaLogoutParameter logoutParameter) {
        LogicUtils.getLogic().ifPresent(logic -> logic._removeTerminal(session, terminal, logoutParameter));
    }

    /**
     * 如果指定账号 id、设备类型的登录客户端已经超过了指定数量，则按照登录时间顺序，把最开始登录的给注销掉
     *
     * @param loginId       账号id
     * @param session       此账号的 Account-Session 对象，可填写 null，框架将自动获取
     * @param deviceType    设备类型（填 null 代表注销此账号所有设备类型的登录）
     * @param maxLoginCount 最大登录数量，超过此数量的将被注销
     * @param logoutMode    超出的客户端将以何种方式被注销
     */
    public void logoutByMaxLoginCount(Object loginId, SaSession session, String deviceType, int maxLoginCount, SaLogoutMode logoutMode) {
        LogicUtils.getLogic().ifPresent(logic -> logic.logoutByMaxLoginCount(loginId, session, deviceType, maxLoginCount, logoutMode));
    }


    // ---- 会话查询

    /**
     * 判断当前会话是否已经登录
     *
     * @return 已登录返回 true，未登录返回 false
     */
    public boolean isLogin() {
        return LogicUtils.getLogic().map(StpLogic::isLogin).orElse(false);
    }

    /**
     * 判断指定账号是否已经登录
     *
     * @return 已登录返回 true，未登录返回 false
     */
    public boolean isLogin(Object loginId) {
        return LogicUtils.getLogic().map(logic -> logic.isLogin(loginId)).orElse(false);
    }

    /**
     * 检验当前会话是否已经登录，如未登录，则抛出异常
     */
    public void checkLogin() {
        LogicUtils.getLogic().ifPresent(StpLogic::checkLogin);
    }

    /**
     * 获取当前会话账号id，如果未登录，则抛出异常
     *
     * @return 账号id
     */
    public Object getLoginId() {
        return LogicUtils.getLogic().map(StpLogic::getLoginId).orElse(null);
    }

    /**
     * 获取当前会话账号id, 如果未登录，则返回默认值
     *
     * @param <T>          返回类型
     * @param defaultValue 默认值
     * @return 登录id
     */
    @SuppressWarnings("unchecked")
    public <T> T getLoginId(T defaultValue) {
        return LogicUtils.getLogic().map(logic -> logic.getLoginId(defaultValue)).orElse(defaultValue);
    }

    /**
     * 获取当前会话账号id, 如果未登录，则返回null
     *
     * @return 账号id
     */
    public Object getLoginIdDefaultNull() {
        return LogicUtils.getLogic().map(StpLogic::getLoginIdDefaultNull).orElse(null);
    }

    /**
     * 获取当前会话账号id, 并转换为 String 类型
     *
     * @return 账号id
     */
    public String getLoginIdAsString() {
        return LogicUtils.getLogic().map(StpLogic::getLoginIdAsString).orElse(null);
    }

    /**
     * 获取当前会话账号id, 并转换为 int 类型
     *
     * @return 账号id
     */
    public int getLoginIdAsInt() {
        return LogicUtils.getLogic().map(StpLogic::getLoginIdAsInt).orElse(0);
    }

    /**
     * 获取当前会话账号id, 并转换为 long 类型
     *
     * @return 账号id
     */
    public long getLoginIdAsLong() {
        return LogicUtils.getLogic().map(StpLogic::getLoginIdAsLong).orElse(0L);
    }

    /**
     * 获取指定 token 对应的账号id，如果 token 无效或 token 处于被踢、被顶、被冻结等状态，则返回 null
     *
     * @param tokenValue token
     * @return 账号id
     */
    public Object getLoginIdByToken(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.getLoginIdByToken(tokenValue)).orElse(null);
    }

    /**
     * 获取指定 token 对应的账号id，如果 token 无效或 token 处于被踢、被顶等状态 (不考虑被冻结)，则返回 null
     *
     * @param tokenValue token
     * @return 账号id
     */
    public Object getLoginIdByTokenNotThinkFreeze(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.getLoginIdByTokenNotThinkFreeze(tokenValue)).orElse(null);
    }

    /**
     * 获取指定 token 对应的账号id （不做任何特殊处理）
     *
     * @param tokenValue token 值
     * @return 账号id
     */
    public String getLoginIdNotHandle(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.getLoginIdNotHandle(tokenValue)).orElse(null);
    }

    /**
     * 获取当前 Token 的扩展信息（此函数只在jwt模式下生效）
     *
     * @param key 键值
     * @return 对应的扩展数据
     */
    public Object getExtra(String key) {
        return LogicUtils.getLogic().map(logic -> logic.getExtra(key)).orElse(null);
    }

    /**
     * 获取指定 Token 的扩展信息（此函数只在jwt模式下生效）
     *
     * @param tokenValue 指定的 Token 值
     * @param key        键值
     * @return 对应的扩展数据
     */
    public Object getExtra(String tokenValue, String key) {
        return LogicUtils.getLogic().map(logic -> logic.getExtra(tokenValue, key)).orElse(null);
    }

    // ---- 其它操作

    /**
     * 判断一个 loginId 是否是有效的 (判断标准：不为 null、空字符串，且不在异常标记项里面)
     *
     * @param loginId 账号id
     * @return /
     */
    public boolean isValidLoginId(Object loginId) {
        return LogicUtils.getLogic().map(logic -> logic.isValidLoginId(loginId)).orElse(false);
    }

    /**
     * 判断一个 token 是否是有效的 (判断标准：使用此 token 查询到的 loginId 不为 Empty )
     *
     * @param tokenValue /
     * @return /
     */
    public boolean isValidToken(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.isValidToken(tokenValue)).orElse(false);
    }

    /**
     * 存储 token - id 映射关系
     *
     * @param tokenValue token值
     * @param loginId    账号id
     * @param timeout    会话有效期 (单位: 秒)
     */
    public void saveTokenToIdMapping(String tokenValue, Object loginId, long timeout) {
        LogicUtils.getLogic().ifPresent(logic -> logic.saveTokenToIdMapping(tokenValue, loginId, timeout));
    }

    /**
     * 更改 token - id 映射关系
     *
     * @param tokenValue token值
     * @param loginId    新的账号Id值
     */
    public void updateTokenToIdMapping(String tokenValue, Object loginId) {
        LogicUtils.getLogic().ifPresent(logic -> logic.updateTokenToIdMapping(tokenValue, loginId));
    }

    /**
     * 删除 token - id 映射
     *
     * @param tokenValue token值
     */
    public void deleteTokenToIdMapping(String tokenValue) {
        LogicUtils.getLogic().ifPresent(logic -> logic.deleteTokenToIdMapping(tokenValue));
    }


    // ------------------- Account-Session 相关 -------------------

    /**
     * 获取指定 key 的 SaSession, 如果该 SaSession 尚未创建，isCreate = 是否立即新建并返回
     *
     * @param sessionId       SessionId
     * @param isCreate        是否新建
     * @param timeout         如果这个 SaSession 是新建的，则使用此值作为过期值（单位：秒），可填 null，代表使用全局 timeout 值
     * @param appendOperation 如果这个 SaSession 是新建的，则要追加执行的动作，可填 null，代表无追加动作
     * @return Session对象
     */
    public SaSession getSessionBySessionId(String sessionId, boolean isCreate, Long timeout, Consumer<SaSession> appendOperation) {
        return LogicUtils.getLogic().map(logic -> logic.getSessionBySessionId(sessionId, isCreate, timeout, appendOperation)).orElse(null);
    }

    /**
     * 获取指定 key 的 SaSession, 如果该 SaSession 尚未创建，则返回 null
     *
     * @param sessionId SessionId
     * @return Session对象
     */
    public SaSession getSessionBySessionId(String sessionId) {
        return LogicUtils.getLogic().map(logic -> logic.getSessionBySessionId(sessionId)).orElse(null);
    }

    /**
     * 获取指定账号 id 的 Account-Session, 如果该 SaSession 尚未创建，isCreate=是否新建并返回
     *
     * @param loginId  账号id
     * @param isCreate 是否新建
     * @param timeout  如果这个 SaSession 是新建的，则使用此值作为过期值（单位：秒），可填 null，代表使用全局 timeout 值
     * @return SaSession 对象
     */
    public SaSession getSessionByLoginId(Object loginId, boolean isCreate, Long timeout) {
        return LogicUtils.getLogic().map(logic -> logic.getSessionByLoginId(loginId, isCreate, timeout)).orElse(null);
    }

    /**
     * 获取指定账号 id 的 Account-Session, 如果该 SaSession 尚未创建，isCreate=是否新建并返回
     *
     * @param loginId  账号id
     * @param isCreate 是否新建
     * @return SaSession 对象
     */
    public SaSession getSessionByLoginId(Object loginId, boolean isCreate) {
        return LogicUtils.getLogic().map(logic -> logic.getSessionByLoginId(loginId, isCreate)).orElse(null);
    }

    /**
     * 获取指定账号 id 的 Account-Session，如果该 SaSession 尚未创建，则新建并返回
     *
     * @param loginId 账号id
     * @return SaSession 对象
     */
    public SaSession getSessionByLoginId(Object loginId) {
        return LogicUtils.getLogic().map(logic -> logic.getSessionByLoginId(loginId)).orElse(null);
    }

    /**
     * 获取当前已登录账号的 Account-Session, 如果该 SaSession 尚未创建，isCreate=是否新建并返回
     *
     * @param isCreate 是否新建
     * @return Session对象
     */
    public SaSession getSession(boolean isCreate) {
        return LogicUtils.getLogic().map(logic -> logic.getSession(isCreate)).orElse(null);
    }

    /**
     * 获取当前已登录账号的 Account-Session，如果该 SaSession 尚未创建，则新建并返回
     *
     * @return Session对象
     */
    public SaSession getSession() {
        return LogicUtils.getLogic().map(StpLogic::getSession).orElse(null);
    }


    // ------------------- Token-Session 相关 -------------------

    /**
     * 获取指定 token 的 Token-Session，如果该 SaSession 尚未创建，isCreate代表是否新建并返回
     *
     * @param tokenValue token值
     * @param isCreate   是否新建
     * @return session对象
     */
    public SaSession getTokenSessionByToken(String tokenValue, boolean isCreate) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenSessionByToken(tokenValue, isCreate)).orElse(null);
    }

    /**
     * 获取指定 token 的 Token-Session，如果该 SaSession 尚未创建，则新建并返回
     *
     * @param tokenValue Token值
     * @return Session对象
     */
    public SaSession getTokenSessionByToken(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenSessionByToken(tokenValue)).orElse(null);
    }

    /**
     * 获取当前 token 的 Token-Session，如果该 SaSession 尚未创建，isCreate代表是否新建并返回
     *
     * @param isCreate 是否新建
     * @return Session对象
     */
    public SaSession getTokenSession(boolean isCreate) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenSession(isCreate)).orElse(null);
    }

    /**
     * 获取当前 token 的 Token-Session，如果该 SaSession 尚未创建，则新建并返回
     *
     * @return Session对象
     */
    public SaSession getTokenSession() {
        return LogicUtils.getLogic().map(StpLogic::getTokenSession).orElse(null);
    }

    /**
     * 获取当前匿名 Token-Session （可在未登录情况下使用的 Token-Session）
     *
     * @param isCreate 在 Token-Session 尚未创建的情况是否新建并返回
     * @return Token-Session 对象
     */
    public SaSession getAnonTokenSession(boolean isCreate) {
        return LogicUtils.getLogic().map(logic -> logic.getAnonTokenSession(isCreate)).orElse(null);
    }

    /**
     * 获取当前匿名 Token-Session （可在未登录情况下使用的Token-Session）
     *
     * @return Token-Session 对象
     */
    public SaSession getAnonTokenSession() {
        return LogicUtils.getLogic().map(StpLogic::getAnonTokenSession).orElse(null);
    }

    /**
     * 删除指定 token 的 Token-Session
     *
     * @param tokenValue token值
     */
    public void deleteTokenSession(String tokenValue) {
        LogicUtils.getLogic().ifPresent(logic -> logic.deleteTokenSession(tokenValue));
    }


    // ------------------- Active-Timeout token 最低活跃度 验证相关 -------------------


    /**
     * 续签指定 token：将这个 token 的 [ 最后活跃时间 ] 更新为当前时间戳
     *
     * @param tokenValue 指定token
     */
    public void updateLastActiveToNow(String tokenValue) {
        LogicUtils.getLogic().ifPresent(logic -> logic.updateLastActiveToNow(tokenValue));
    }

    /**
     * 续签当前 token：(将 [最后操作时间] 更新为当前时间戳)
     * <h2>
     * 请注意: 即使 token 已被冻结 也可续签成功，
     * 如果此场景下需要提示续签失败，可在此之前调用 checkActiveTimeout() 强制检查是否冻结即可
     * </h2>
     */
    public void updateLastActiveToNow() {
        LogicUtils.getLogic().ifPresent(StpLogic::updateLastActiveToNow);
    }


    /**
     * 判断指定 token 是否已被冻结
     *
     * @param tokenValue 指定 token
     */
    public boolean isFreeze(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.isFreeze(tokenValue)).orElse(false);
    }

    /**
     * 根据全局配置决定是否校验指定 token 的活跃度
     *
     * @param tokenValue 指定 token
     */
    public void checkActiveTimeoutByConfig(String tokenValue) {
        LogicUtils.getLogic().ifPresent(logic -> logic.checkActiveTimeoutByConfig(tokenValue));
    }

    /**
     * 检查指定 token 是否已被冻结，如果是则抛出异常
     *
     * @param tokenValue 指定 token
     */
    public void checkActiveTimeout(String tokenValue) {
        LogicUtils.getLogic().ifPresent(logic -> logic.checkActiveTimeout(tokenValue));
    }

    /**
     * 检查当前 token 是否已被冻结，如果是则抛出异常
     */
    public void checkActiveTimeout() {
        LogicUtils.getLogic().ifPresent(StpLogic::checkActiveTimeout);
    }

    /**
     * 获取指定 token 在缓存中的 activeTimeout 值，如果不存在则返回 null
     *
     * @param tokenValue 指定token
     * @return /
     */
    public Long getTokenUseActiveTimeout(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenUseActiveTimeout(tokenValue)).orElse(null);
    }

    /**
     * 获取指定 token 在缓存中的 activeTimeout 值，如果不存在则返回全局配置的 activeTimeout 值
     *
     * @param tokenValue 指定token
     * @return /
     */
    public long getTokenUseActiveTimeoutOrGlobalConfig(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenUseActiveTimeoutOrGlobalConfig(tokenValue)).orElse(-2L);
    }

    /**
     * 获取指定 token 的最后活跃时间（13位时间戳），如果不存在则返回 -2
     *
     * @param tokenValue 指定token
     * @return /
     */
    public long getTokenLastActiveTime(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenLastActiveTime(tokenValue)).orElse(-2L);
    }

    /**
     * 获取当前 token 的最后活跃时间（13位时间戳），如果不存在则返回 -2
     *
     * @return /
     */
    public long getTokenLastActiveTime() {
        return LogicUtils.getLogic().map(StpLogic::getTokenLastActiveTime).orElse(-2L);
    }


    // ------------------- 过期时间相关 -------------------

    /**
     * 获取当前会话 token 剩余有效时间（单位: 秒，返回 -1 代表永久有效，-2 代表没有这个值）
     *
     * @return token剩余有效时间
     */
    public long getTokenTimeout() {
        return LogicUtils.getLogic().map(StpLogic::getTokenTimeout).orElse(-2L);
    }

    /**
     * 获取指定 token 剩余有效时间（单位: 秒，返回 -1 代表永久有效，-2 代表没有这个值）
     *
     * @param token 指定token
     * @return token剩余有效时间
     */
    public long getTokenTimeout(String token) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenTimeout(token)).orElse(-2L);
    }

    /**
     * 获取指定账号 id 的 token 剩余有效时间（单位: 秒，返回 -1 代表永久有效，-2 代表没有这个值）
     *
     * @param loginId 指定loginId
     * @return token剩余有效时间
     */
    public long getTokenTimeoutByLoginId(Object loginId) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenTimeoutByLoginId(loginId)).orElse(-2L);
    }

    /**
     * 获取当前登录账号的 Account-Session 剩余有效时间（单位: 秒，返回 -1 代表永久有效，-2 代表没有这个值）
     *
     * @return token剩余有效时间
     */
    public long getSessionTimeout() {
        return LogicUtils.getLogic().map(StpLogic::getSessionTimeout).orElse(-2L);
    }

    /**
     * 获取指定账号 id 的 Account-Session 剩余有效时间（单位: 秒，返回 -1 代表永久有效，-2 代表没有这个值）
     *
     * @param loginId 指定loginId
     * @return token剩余有效时间
     */
    public long getSessionTimeoutByLoginId(Object loginId) {
        return LogicUtils.getLogic().map(logic -> logic.getSessionTimeoutByLoginId(loginId)).orElse(-2L);
    }

    /**
     * 获取当前 token 的 Token-Session 剩余有效时间（单位: 秒，返回 -1 代表永久有效，-2 代表没有这个值）
     *
     * @return token剩余有效时间
     */
    public long getTokenSessionTimeout() {
        return LogicUtils.getLogic().map(StpLogic::getTokenSessionTimeout).orElse(-2L);
    }

    /**
     * 获取指定 token 的 Token-Session 剩余有效时间（单位: 秒，返回 -1 代表永久有效，-2 代表没有这个值）
     *
     * @param tokenValue 指定token
     * @return token 剩余有效时间
     */
    public long getTokenSessionTimeoutByTokenValue(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenSessionTimeoutByTokenValue(tokenValue)).orElse(-2L);
    }

    /**
     * 获取当前 token 剩余活跃有效期：当前 token 距离被冻结还剩多少时间（单位: 秒，返回 -1 代表永不冻结，-2 代表没有这个值或 token 已被冻结了）
     *
     * @return /
     */
    public long getTokenActiveTimeout() {
        return LogicUtils.getLogic().map(StpLogic::getTokenActiveTimeout).orElse(-2L);
    }

    /**
     * 获取指定 token 剩余活跃有效期：这个 token 距离被冻结还剩多少时间（单位: 秒，返回 -1 代表永不冻结，-2 代表没有这个值或 token 已被冻结了）
     *
     * @param tokenValue 指定 token
     * @return /
     */
    public long getTokenActiveTimeoutByToken(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenActiveTimeoutByToken(tokenValue)).orElse(-2L);
    }

    /**
     * 对当前 token 的 timeout 值进行续期
     *
     * @param timeout 要修改成为的有效时间 (单位: 秒)
     */
    public void renewTimeout(long timeout) {
        LogicUtils.getLogic().ifPresent(logic -> logic.renewTimeout(timeout));
    }

    /**
     * 对指定 token 的 timeout 值进行续期
     *
     * @param tokenValue 指定 token
     * @param timeout    要修改成为的有效时间 (单位: 秒，填 -1 代表要续为永久有效)
     */
    public void renewTimeout(String tokenValue, long timeout) {
        LogicUtils.getLogic().ifPresent(logic -> logic.renewTimeout(tokenValue, timeout));
    }


    // ------------------- 角色认证操作 -------------------

    /**
     * 获取：当前账号的角色集合
     *
     * @return /
     */
    public List<String> getRoleList() {
        return LogicUtils.getLogic().map(StpLogic::getRoleList).orElse(null);
    }

    /**
     * 获取：指定账号的角色集合
     *
     * @param loginId 指定账号id
     * @return /
     */
    public List<String> getRoleList(Object loginId) {
        return LogicUtils.getLogic().map(logic -> logic.getRoleList(loginId)).orElse(null);
    }

    /**
     * 判断：当前账号是否拥有指定角色, 返回 true 或 false
     *
     * @param role 角色
     * @return /
     */
    public boolean hasRole(String role) {
        return LogicUtils.getLogic().map(logic -> logic.hasRole(role)).orElse(false);
    }

    /**
     * 判断：指定账号是否含有指定角色标识, 返回 true 或 false
     *
     * @param loginId 账号id
     * @param role    角色标识
     * @return 是否含有指定角色标识
     */
    public boolean hasRole(Object loginId, String role) {
        return LogicUtils.getLogic().map(logic -> logic.hasRole(loginId, role)).orElse(false);
    }

    /**
     * 判断：当前账号是否含有指定角色标识 [ 指定多个，必须全部验证通过 ]
     *
     * @param roleArray 角色标识数组
     * @return true或false
     */
    public boolean hasRoleAnd(String... roleArray) {
        return LogicUtils.getLogic().map(logic -> logic.hasRoleAnd(roleArray)).orElse(false);
    }

    /**
     * 判断：当前账号是否含有指定角色标识 [ 指定多个，只要其一验证通过即可 ]
     *
     * @param roleArray 角色标识数组
     * @return true或false
     */
    public boolean hasRoleOr(String... roleArray) {
        return LogicUtils.getLogic().map(logic -> logic.hasRoleOr(roleArray)).orElse(false);
    }

    /**
     * 校验：当前账号是否含有指定角色标识, 如果验证未通过，则抛出异常: NotRoleException
     *
     * @param role 角色标识
     */
    public void checkRole(String role) {
        LogicUtils.getLogic().ifPresent(logic -> logic.checkRole(role));
    }

    /**
     * 校验：当前账号是否含有指定角色标识 [ 指定多个，必须全部验证通过 ]
     *
     * @param roleArray 角色标识数组
     */
    public void checkRoleAnd(String... roleArray) {
        LogicUtils.getLogic().ifPresent(logic -> logic.checkRoleAnd(roleArray));
    }

    /**
     * 校验：当前账号是否含有指定角色标识 [ 指定多个，只要其一验证通过即可 ]
     *
     * @param roleArray 角色标识数组
     */
    public void checkRoleOr(String... roleArray) {
        LogicUtils.getLogic().ifPresent(logic -> logic.checkRoleOr(roleArray));
    }


    // ------------------- 权限认证操作 -------------------

    /**
     * 获取：当前账号的权限码集合
     *
     * @return /
     */
    public List<String> getPermissionList() {
        return LogicUtils.getLogic().map(StpLogic::getPermissionList).orElse(null);
    }

    /**
     * 获取：指定账号的权限码集合
     *
     * @param loginId 指定账号id
     * @return /
     */
    public List<String> getPermissionList(Object loginId) {
        return LogicUtils.getLogic().map(logic -> logic.getPermissionList(loginId)).orElse(null);
    }

    /**
     * 判断：当前账号是否含有指定权限, 返回 true 或 false
     *
     * @param permission 权限码
     * @return 是否含有指定权限
     */
    public boolean hasPermission(String permission) {
        return LogicUtils.getLogic().map(logic -> logic.hasPermission(permission)).orElse(false);
    }

    /**
     * 判断：指定账号 id 是否含有指定权限, 返回 true 或 false
     *
     * @param loginId    账号 id
     * @param permission 权限码
     * @return 是否含有指定权限
     */
    public boolean hasPermission(Object loginId, String permission) {
        return LogicUtils.getLogic().map(logic -> logic.hasPermission(loginId, permission)).orElse(false);
    }

    /**
     * 判断：当前账号是否含有指定权限 [ 指定多个，必须全部具有 ]
     *
     * @param permissionArray 权限码数组
     * @return true 或 false
     */
    public boolean hasPermissionAnd(String... permissionArray) {
        return LogicUtils.getLogic().map(logic -> logic.hasPermissionAnd(permissionArray)).orElse(false);
    }

    /**
     * 判断：当前账号是否含有指定权限 [ 指定多个，只要其一验证通过即可 ]
     *
     * @param permissionArray 权限码数组
     * @return true 或 false
     */
    public boolean hasPermissionOr(String... permissionArray) {
        return LogicUtils.getLogic().map(logic -> logic.hasPermissionOr(permissionArray)).orElse(false);
    }

    /**
     * 校验：当前账号是否含有指定权限, 如果验证未通过，则抛出异常: NotPermissionException
     *
     * @param permission 权限码
     */
    public void checkPermission(String permission) {
        LogicUtils.getLogic().ifPresent(logic -> logic.checkPermission(permission));
    }

    /**
     * 校验：当前账号是否含有指定权限 [ 指定多个，必须全部验证通过 ]
     *
     * @param permissionArray 权限码数组
     */
    public void checkPermissionAnd(String... permissionArray) {
        LogicUtils.getLogic().ifPresent(logic -> logic.checkPermissionAnd(permissionArray));
    }

    /**
     * 校验：当前账号是否含有指定权限 [ 指定多个，只要其一验证通过即可 ]
     *
     * @param permissionArray 权限码数组
     */
    public void checkPermissionOr(String... permissionArray) {
        LogicUtils.getLogic().ifPresent(logic -> logic.checkPermissionOr(permissionArray));
    }


    // ------------------- id 反查 token 相关操作 -------------------

    /**
     * 获取指定账号 id 的 token
     * <p>
     * 在配置为允许并发登录时，此方法只会返回队列的最后一个 token，
     * 如果你需要返回此账号 id 的所有 token，请调用 getTokenValueListByLoginId
     * </p>
     *
     * @param loginId 账号id
     * @return token值
     */
    public String getTokenValueByLoginId(Object loginId) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenValueByLoginId(loginId)).orElse(null);
    }

    /**
     * 获取指定账号 id 指定设备类型端的 token
     * <p>
     * 在配置为允许并发登录时，此方法只会返回队列的最后一个 token，
     * 如果你需要返回此账号 id 的所有 token，请调用 getTokenValueListByLoginId
     * </p>
     *
     * @param loginId    账号id
     * @param deviceType 设备类型，填 null 代表不限设备类型
     * @return token值
     */
    public String getTokenValueByLoginId(Object loginId, String deviceType) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenValueByLoginId(loginId, deviceType)).orElse(null);
    }

    /**
     * 获取指定账号 id 的 token 集合
     *
     * @param loginId 账号id
     * @return 此 loginId 的所有相关 token
     */
    public List<String> getTokenValueListByLoginId(Object loginId) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenValueListByLoginId(loginId)).orElse(null);
    }

    /**
     * 获取指定账号 id 指定设备类型端的 token 集合
     *
     * @param loginId    账号id
     * @param deviceType 设备类型，填 null 代表不限设备类型
     * @return 此 loginId 的所有登录 token
     */
    public List<String> getTokenValueListByLoginId(Object loginId, String deviceType) {
        return LogicUtils.getLogic().map(logic -> logic.getTokenValueListByLoginId(loginId, deviceType)).orElse(null);
    }

    /**
     * 获取指定账号 id 已登录设备信息集合
     *
     * @param loginId 账号id
     * @return 此 loginId 的所有登录 token
     */
    public List<SaTerminalInfo> getTerminalListByLoginId(Object loginId) {
        return LogicUtils.getLogic().map(logic -> logic.getTerminalListByLoginId(loginId)).orElse(null);
    }

    /**
     * 获取指定账号 id 指定设备类型端的已登录设备信息集合
     *
     * @param loginId    账号id
     * @param deviceType 设备类型，填 null 代表不限设备类型
     * @return 此 loginId 的所有登录 token
     */
    public List<SaTerminalInfo> getTerminalListByLoginId(Object loginId, String deviceType) {
        return LogicUtils.getLogic().map(logic -> logic.getTerminalListByLoginId(loginId, deviceType)).orElse(null);
    }

    /**
     * 获取指定账号 id 已登录设备信息集合，执行特定函数
     *
     * @param loginId  账号id
     * @param function 需要执行的函数
     */
    public void forEachTerminalList(Object loginId, cn.dev33.satoken.fun.SaTwoParamFunction<SaSession, SaTerminalInfo> function) {
        LogicUtils.getLogic().ifPresent(logic -> logic.forEachTerminalList(loginId, function));
    }

    /**
     * 返回当前 token 指向的 SaTerminalInfo 设备信息，如果 token 无效则返回 null
     *
     * @return /
     */
    public SaTerminalInfo getTerminalInfo() {
        return LogicUtils.getLogic().map(StpLogic::getTerminalInfo).orElse(null);
    }

    /**
     * 返回指定 token 指向的 SaTerminalInfo 设备信息，如果 Token 无效则返回 null
     *
     * @param tokenValue 指定 token
     * @return /
     */
    public SaTerminalInfo getTerminalInfoByToken(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.getTerminalInfoByToken(tokenValue)).orElse(null);
    }

    /**
     * 返回当前会话的登录设备类型
     *
     * @return 当前令牌的登录设备类型
     */
    public String getLoginDeviceType() {
        return LogicUtils.getLogic().map(StpLogic::getLoginDeviceType).orElse(null);
    }

    /**
     * 返回指定 token 会话的登录设备类型
     *
     * @param tokenValue 指定token
     * @return 当前令牌的登录设备类型
     */
    public String getLoginDeviceTypeByToken(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.getLoginDeviceTypeByToken(tokenValue)).orElse(null);
    }

    /**
     * 返回当前会话的登录设备 ID
     *
     * @return /
     */
    public String getLoginDeviceId() {
        return LogicUtils.getLogic().map(StpLogic::getLoginDeviceId).orElse(null);
    }

    /**
     * 返回指定 token 会话的登录设备 ID
     *
     * @param tokenValue 指定token
     * @return /
     */
    public String getLoginDeviceIdByToken(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.getLoginDeviceIdByToken(tokenValue)).orElse(null);
    }

    /**
     * 判断对于指定 loginId 来讲，指定设备 id 是否为可信任设备
     *
     * @param deviceId /
     * @return /
     */
    public boolean isTrustDeviceId(Object userId, String deviceId) {
        return LogicUtils.getLogic().map(logic -> logic.isTrustDeviceId(userId, deviceId)).orElse(false);
    }


    // ------------------- 会话管理 -------------------

    /**
     * 根据条件查询缓存中所有的 token
     *
     * @param keyword  关键字
     * @param start    开始处索引
     * @param size     获取数量 (-1代表一直获取到末尾)
     * @param sortType 排序类型（true=正序，false=反序）
     * @return token集合
     */
    public List<String> searchTokenValue(String keyword, int start, int size, boolean sortType) {
        return LogicUtils.getLogic().map(logic -> logic.searchTokenValue(keyword, start, size, sortType)).orElse(null);
    }

    /**
     * 根据条件查询缓存中所有的 SessionId
     *
     * @param keyword  关键字
     * @param start    开始处索引
     * @param size     获取数量  (-1代表一直获取到末尾)
     * @param sortType 排序类型（true=正序，false=反序）
     * @return sessionId集合
     */
    public List<String> searchSessionId(String keyword, int start, int size, boolean sortType) {
        return LogicUtils.getLogic().map(logic -> logic.searchSessionId(keyword, start, size, sortType)).orElse(null);
    }

    /**
     * 根据条件查询缓存中所有的 Token-Session-Id
     *
     * @param keyword  关键字
     * @param start    开始处索引
     * @param size     获取数量 (-1代表一直获取到末尾)
     * @param sortType 排序类型（true=正序，false=反序）
     * @return sessionId集合
     */
    public List<String> searchTokenSessionId(String keyword, int start, int size, boolean sortType) {
        return LogicUtils.getLogic().map(logic -> logic.searchTokenSessionId(keyword, start, size, sortType)).orElse(null);
    }


    // ------------------- 账号封禁 -------------------

    /**
     * 封禁：指定账号
     * <p> 此方法不会直接将此账号id踢下线，如需封禁后立即掉线，请追加调用 StpUtil.logout(id)
     *
     * @param loginId 指定账号id
     * @param time    封禁时间, 单位: 秒 （-1=永久封禁）
     */
    public void disable(Object loginId, long time) {
        LogicUtils.getLogic().ifPresent(logic -> logic.disable(loginId, time));
    }

    /**
     * 判断：指定账号是否已被封禁 (true=已被封禁, false=未被封禁)
     *
     * @param loginId 账号id
     * @return /
     */
    public boolean isDisable(Object loginId) {
        return LogicUtils.getLogic().map(logic -> logic.isDisable(loginId)).orElse(false);
    }

    /**
     * 校验：指定账号是否已被封禁，如果被封禁则抛出异常
     *
     * @param loginId 账号id
     */
    public void checkDisable(Object loginId) {
        LogicUtils.getLogic().ifPresent(logic -> logic.checkDisable(loginId));
    }

    /**
     * 获取：指定账号剩余封禁时间，单位：秒（-1=永久封禁，-2=未被封禁）
     *
     * @param loginId 账号id
     * @return /
     */
    public long getDisableTime(Object loginId) {
        return LogicUtils.getLogic().map(logic -> logic.getDisableTime(loginId)).orElse(-2L);
    }

    /**
     * 解封：指定账号
     *
     * @param loginId 账号id
     */
    public void untieDisable(Object loginId) {
        LogicUtils.getLogic().ifPresent(logic -> logic.untieDisable(loginId));
    }


    // ------------------- 分类封禁 -------------------

    /**
     * 封禁：指定账号的指定服务
     * <p> 此方法不会直接将此账号id踢下线，如需封禁后立即掉线，请追加调用 StpUtil.logout(id)
     *
     * @param loginId 指定账号id
     * @param service 指定服务
     * @param time    封禁时间, 单位: 秒 （-1=永久封禁）
     */
    public void disable(Object loginId, String service, long time) {
        LogicUtils.getLogic().ifPresent(logic -> logic.disable(loginId, service, time));
    }

    /**
     * 判断：指定账号的指定服务 是否已被封禁（true=已被封禁, false=未被封禁）
     *
     * @param loginId 账号id
     * @param service 指定服务
     * @return /
     */
    public boolean isDisable(Object loginId, String service) {
        return LogicUtils.getLogic().map(logic -> logic.isDisable(loginId, service)).orElse(false);
    }

    /**
     * 校验：指定账号 指定服务 是否已被封禁，如果被封禁则抛出异常
     *
     * @param loginId  账号id
     * @param services 指定服务，可以指定多个
     */
    public void checkDisable(Object loginId, String... services) {
        LogicUtils.getLogic().ifPresent(logic -> logic.checkDisable(loginId, services));
    }

    /**
     * 获取：指定账号 指定服务 剩余封禁时间，单位：秒（-1=永久封禁，-2=未被封禁）
     *
     * @param loginId 账号id
     * @param service 指定服务
     * @return see note
     */
    public long getDisableTime(Object loginId, String service) {
        return LogicUtils.getLogic().map(logic -> logic.getDisableTime(loginId, service)).orElse(-2L);
    }

    /**
     * 解封：指定账号、指定服务
     *
     * @param loginId  账号id
     * @param services 指定服务，可以指定多个
     */
    public void untieDisable(Object loginId, String... services) {
        LogicUtils.getLogic().ifPresent(logic -> logic.untieDisable(loginId, services));
    }


    // ------------------- 阶梯封禁 -------------------

    /**
     * 封禁：指定账号，并指定封禁等级
     *
     * @param loginId 指定账号id
     * @param level   指定封禁等级
     * @param time    封禁时间, 单位: 秒 （-1=永久封禁）
     */
    public void disableLevel(Object loginId, int level, long time) {
        LogicUtils.getLogic().ifPresent(logic -> logic.disableLevel(loginId, level, time));
    }

    /**
     * 封禁：指定账号的指定服务，并指定封禁等级
     *
     * @param loginId 指定账号id
     * @param service 指定封禁服务
     * @param level   指定封禁等级
     * @param time    封禁时间, 单位: 秒 （-1=永久封禁）
     */
    public void disableLevel(Object loginId, String service, int level, long time) {
        LogicUtils.getLogic().ifPresent(logic -> logic.disableLevel(loginId, service, level, time));
    }

    /**
     * 判断：指定账号是否已被封禁到指定等级
     *
     * @param loginId 指定账号id
     * @param level   指定封禁等级
     * @return /
     */
    public boolean isDisableLevel(Object loginId, int level) {
        return LogicUtils.getLogic().map(logic -> logic.isDisableLevel(loginId, level)).orElse(false);
    }

    /**
     * 判断：指定账号的指定服务，是否已被封禁到指定等级
     *
     * @param loginId 指定账号id
     * @param service 指定封禁服务
     * @param level   指定封禁等级
     * @return /
     */
    public boolean isDisableLevel(Object loginId, String service, int level) {
        return LogicUtils.getLogic().map(logic -> logic.isDisableLevel(loginId, service, level)).orElse(false);
    }

    /**
     * 校验：指定账号是否已被封禁到指定等级（如果已经达到，则抛出异常）
     *
     * @param loginId 指定账号id
     * @param level   封禁等级 （只有 封禁等级 ≥ 此值 才会抛出异常）
     */
    public void checkDisableLevel(Object loginId, int level) {
        LogicUtils.getLogic().ifPresent(logic -> logic.checkDisableLevel(loginId, level));
    }

    /**
     * 校验：指定账号的指定服务，是否已被封禁到指定等级（如果已经达到，则抛出异常）
     *
     * @param loginId 指定账号id
     * @param service 指定封禁服务
     * @param level   封禁等级 （只有 封禁等级 ≥ 此值 才会抛出异常）
     */
    public void checkDisableLevel(Object loginId, String service, int level) {
        LogicUtils.getLogic().ifPresent(logic -> logic.checkDisableLevel(loginId, service, level));
    }

    /**
     * 获取：指定账号被封禁的等级，如果未被封禁则返回-2
     *
     * @param loginId 指定账号id
     * @return /
     */
    public int getDisableLevel(Object loginId) {
        return LogicUtils.getLogic().map(logic -> logic.getDisableLevel(loginId)).orElse(-2);
    }

    /**
     * 获取：指定账号的 指定服务 被封禁的等级，如果未被封禁则返回-2
     *
     * @param loginId 指定账号id
     * @param service 指定封禁服务
     * @return /
     */
    public int getDisableLevel(Object loginId, String service) {
        return LogicUtils.getLogic().map(logic -> logic.getDisableLevel(loginId, service)).orElse(-2);
    }


    // ------------------- 临时身份切换 -------------------

    /**
     * 临时切换身份为指定账号id
     *
     * @param loginId 指定loginId
     */
    public void switchTo(Object loginId) {
        LogicUtils.getLogic().ifPresent(logic -> logic.switchTo(loginId));
    }

    /**
     * 结束临时切换身份
     */
    public void endSwitch() {
        LogicUtils.getLogic().ifPresent(StpLogic::endSwitch);
    }

    /**
     * 判断当前请求是否正处于 [ 身份临时切换 ] 中
     *
     * @return /
     */
    public boolean isSwitch() {
        return LogicUtils.getLogic().map(StpLogic::isSwitch).orElse(false);
    }

    /**
     * 返回 [ 身份临时切换 ] 的 loginId
     *
     * @return /
     */
    public Object getSwitchLoginId() {
        return LogicUtils.getLogic().map(StpLogic::getSwitchLoginId).orElse(null);
    }

    /**
     * 在一个 lambda 代码段里，临时切换身份为指定账号id，lambda 结束后自动恢复
     *
     * @param loginId  指定账号id
     * @param function 要执行的方法
     */
    public void switchTo(Object loginId, cn.dev33.satoken.fun.SaFunction function) {
        LogicUtils.getLogic().ifPresent(logic -> logic.switchTo(loginId, function));
    }


    // ------------------- 二级认证 -------------------

    /**
     * 在当前会话 开启二级认证
     *
     * @param safeTime 维持时间 (单位: 秒)
     */
    public void openSafe(long safeTime) {
        LogicUtils.getLogic().ifPresent(logic -> logic.openSafe(safeTime));
    }

    /**
     * 在当前会话 开启二级认证
     *
     * @param service  业务标识
     * @param safeTime 维持时间 (单位: 秒)
     */
    public void openSafe(String service, long safeTime) {
        LogicUtils.getLogic().ifPresent(logic -> logic.openSafe(service, safeTime));
    }

    /**
     * 判断：当前会话是否处于二级认证时间内
     *
     * @return true=二级认证已通过, false=尚未进行二级认证或认证已超时
     */
    public boolean isSafe() {
        return LogicUtils.getLogic().map(StpLogic::isSafe).orElse(false);
    }

    /**
     * 判断：当前会话 是否处于指定业务的二级认证时间内
     *
     * @param service 业务标识
     * @return true=二级认证已通过, false=尚未进行二级认证或认证已超时
     */
    public boolean isSafe(String service) {
        return LogicUtils.getLogic().map(logic -> logic.isSafe(service)).orElse(false);
    }

    /**
     * 判断：指定 token 是否处于二级认证时间内
     *
     * @param tokenValue Token 值
     * @param service    业务标识
     * @return true=二级认证已通过, false=尚未进行二级认证或认证已超时
     */
    public boolean isSafe(String tokenValue, String service) {
        return LogicUtils.getLogic().map(logic -> logic.isSafe(tokenValue, service)).orElse(false);
    }

    /**
     * 校验：当前会话是否已通过二级认证，如未通过则抛出异常
     */
    public void checkSafe() {
        LogicUtils.getLogic().ifPresent(StpLogic::checkSafe);
    }

    /**
     * 校验：检查当前会话是否已通过指定业务的二级认证，如未通过则抛出异常
     *
     * @param service 业务标识
     */
    public void checkSafe(String service) {
        LogicUtils.getLogic().ifPresent(logic -> logic.checkSafe(service));
    }

    /**
     * 获取：当前会话的二级认证剩余有效时间（单位: 秒, 返回-2代表尚未通过二级认证）
     *
     * @return 剩余有效时间
     */
    public long getSafeTime() {
        return LogicUtils.getLogic().map(StpLogic::getSafeTime).orElse(-2L);
    }

    /**
     * 获取：当前会话的二级认证剩余有效时间（单位: 秒, 返回-2代表尚未通过二级认证）
     *
     * @param service 业务标识
     * @return 剩余有效时间
     */
    public long getSafeTime(String service) {
        return LogicUtils.getLogic().map(logic -> logic.getSafeTime(service)).orElse(-2L);
    }

    /**
     * 在当前会话 结束二级认证
     */
    public void closeSafe() {
        LogicUtils.getLogic().ifPresent(StpLogic::closeSafe);
    }

    /**
     * 在当前会话 结束指定业务标识的二级认证
     *
     * @param service 业务标识
     */
    public void closeSafe(String service) {
        LogicUtils.getLogic().ifPresent(logic -> logic.closeSafe(service));
    }


    // ------------------- 拼接相应key -------------------

    /**
     * 获取：客户端 tokenName
     *
     * @return key
     */
    public String splicingKeyTokenName() {
        return LogicUtils.getLogic().map(StpLogic::splicingKeyTokenName).orElse(null);
    }

    /**
     * 拼接： 在保存 token - id 映射关系时，应该使用的key
     *
     * @param tokenValue token值
     * @return key
     */
    public String splicingKeyTokenValue(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.splicingKeyTokenValue(tokenValue)).orElse(null);
    }

    /**
     * 拼接： 在保存 Account-Session 时，应该使用的 key
     *
     * @param loginId 账号id
     * @return key
     */
    public String splicingKeySession(Object loginId) {
        return LogicUtils.getLogic().map(logic -> logic.splicingKeySession(loginId)).orElse(null);
    }

    /**
     * 拼接：在保存 Token-Session 时，应该使用的 key
     *
     * @param tokenValue token值
     * @return key
     */
    public String splicingKeyTokenSession(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.splicingKeyTokenSession(tokenValue)).orElse(null);
    }

    /**
     * 拼接： 在保存 token 最后活跃时间时，应该使用的 key
     *
     * @param tokenValue token值
     * @return key
     */
    public String splicingKeyLastActiveTime(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.splicingKeyLastActiveTime(tokenValue)).orElse(null);
    }

    /**
     * 拼接：在进行临时身份切换时，应该使用的 key
     *
     * @return key
     */
    public String splicingKeySwitch() {
        return LogicUtils.getLogic().map(StpLogic::splicingKeySwitch).orElse(null);
    }

    /**
     * 如果 token 为本次请求新创建的，则以此字符串为 key 存储在当前 request 中
     *
     * @return key
     */
    public String splicingKeyJustCreatedSave() {
        return LogicUtils.getLogic().map(StpLogic::splicingKeyJustCreatedSave).orElse(null);
    }

    /**
     * 拼接： 在保存服务封禁标记时，应该使用的 key
     *
     * @param loginId 账号id
     * @param service 具体封禁的服务
     * @return key
     */
    public String splicingKeyDisable(Object loginId, String service) {
        return LogicUtils.getLogic().map(logic -> logic.splicingKeyDisable(loginId, service)).orElse(null);
    }

    /**
     * 拼接： 在保存业务二级认证标记时，应该使用的 key
     *
     * @param tokenValue 要认证的 Token
     * @param service    要认证的业务标识
     * @return key
     */
    public String splicingKeySafe(String tokenValue, String service) {
        return LogicUtils.getLogic().map(logic -> logic.splicingKeySafe(tokenValue, service)).orElse(null);
    }


    // ------------------- Bean 对象、字段代理 -------------------

    /**
     * 返回当前 StpLogic 使用的持久化对象
     *
     * @return /
     */
    public cn.dev33.satoken.dao.SaTokenDao getSaTokenDao() {
        return LogicUtils.getLogic().map(StpLogic::getSaTokenDao).orElse(null);
    }

    /**
     * 返回当前 StpLogic 是否支持共享 token 策略
     *
     * @return /
     */
    public boolean isSupportShareToken() {
        return LogicUtils.getLogic().map(StpLogic::isSupportShareToken).orElse(false);
    }

    /**
     * 返回全局配置是否开启了 Token 活跃度校验，返回 true 代表已打开，返回 false 代表不打开，此时永不冻结 token
     *
     * @return /
     */
    public boolean isOpenCheckActiveTimeout() {
        return LogicUtils.getLogic().map(StpLogic::isOpenCheckActiveTimeout).orElse(false);
    }

    /**
     * 返回全局配置的 Cookie 保存时长，单位：秒 （根据全局 timeout 计算）
     *
     * @return Cookie 应该保存的时长
     */
    public int getConfigOfCookieTimeout() {
        return LogicUtils.getLogic().map(StpLogic::getConfigOfCookieTimeout).orElse(0);
    }

    /**
     * 返回全局配置的 maxTryTimes 值，在每次创建 token 时，对其唯一性测试的最高次数（-1=不测试）
     *
     * @param loginParameter /
     * @return /
     */
    public int getConfigOfMaxTryTimes(SaLoginParameter loginParameter) {
        return LogicUtils.getLogic().map(logic -> logic.getConfigOfMaxTryTimes(loginParameter)).orElse(-1);
    }

    /**
     * 判断：集合中是否包含指定元素（模糊匹配）
     *
     * @param list    集合
     * @param element 元素
     * @return /
     */
    public boolean hasElement(List<String> list, String element) {
        return LogicUtils.getLogic().map(logic -> logic.hasElement(list, element)).orElse(false);
    }

    /**
     * 当前 StpLogic 对象是否支持 token 扩展参数
     *
     * @return /
     */
    public boolean isSupportExtra() {
        return LogicUtils.getLogic().map(StpLogic::isSupportExtra).orElse(false);
    }

    /**
     * 根据当前配置对象创建一个 SaLoginParameter 对象
     *
     * @return /
     */
    public SaLoginParameter createSaLoginParameter() {
        return LogicUtils.getLogic().map(StpLogic::createSaLoginParameter).orElse(null);
    }

    /**
     * 根据当前配置对象创建一个 SaLogoutParameter 对象
     *
     * @return /
     */
    public SaLogoutParameter createSaLogoutParameter() {
        return LogicUtils.getLogic().map(StpLogic::createSaLogoutParameter).orElse(null);
    }


    // ------------------- 过期方法 -------------------

    /**
     * <h2>请更换为 getLoginDeviceType </h2>
     * 返回当前会话的登录设备类型
     *
     * @return 当前令牌的登录设备类型
     */

    public String getLoginDevice() {
        return LogicUtils.getLogic().map(StpLogic::getLoginDevice).orElse(null);
    }

    /**
     * <h2>请更换为 getLoginDeviceTypeByToken </h2>
     * 返回指定 token 会话的登录设备类型
     *
     * @param tokenValue 指定token
     * @return 当前令牌的登录设备类型
     */

    public String getLoginDeviceByToken(String tokenValue) {
        return LogicUtils.getLogic().map(logic -> logic.getLoginDeviceByToken(tokenValue)).orElse(null);
    }

}
