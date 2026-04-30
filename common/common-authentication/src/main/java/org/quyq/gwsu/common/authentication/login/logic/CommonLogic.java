package org.quyq.gwsu.common.authentication.login.logic;


import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.util.SaFoxUtil;
import cn.dev33.satoken.util.SaTokenConsts;
import org.quyq.gwsu.common.security.constants.SecurityConstants;

/**
 * @author Quyq
 * @date 2026/4/7
 * @description
 */
public class CommonLogic extends StpLogic {
    /**
     * 初始化 StpLogic, 并指定账号类型
     *
     * @param loginType 账号类型标识
     */
    public CommonLogic(String loginType) {
        super(loginType);
    }


    @Override
    public String getTokenValueNotCut() {
        // 获取相应对象
        SaStorage storage = SaHolder.getStorage();
        SaRequest request = SaHolder.getRequest();
        SaTokenConfig config = getConfigOrGlobal();
        String keyTokenName = SecurityConstants.Authentication.HTTP_HEADER_TOKEN_KEY;
        String tokenValue = null;

        // 1. 先尝试从 Storage 存储器里读取
        if (storage.get(splicingKeyJustCreatedSave()) != null) {
            tokenValue = String.valueOf(storage.get(splicingKeyJustCreatedSave()));
        }
        // 2. 再尝试从 请求体 里面读取
        if (SaFoxUtil.isEmpty(tokenValue) && config.getIsReadBody()) {
            tokenValue = request.getParam(keyTokenName);
        }
        // 3. 再尝试从 header 头里读取
        if (SaFoxUtil.isEmpty(tokenValue) && config.getIsReadHeader()) {
            tokenValue = request.getHeader(keyTokenName);
        }
        // 4. 最后尝试从 cookie 里读取
        if (SaFoxUtil.isEmpty(tokenValue) && config.getIsReadCookie()) {
            tokenValue = request.getCookieValue(getTokenName());
            if (SaFoxUtil.isNotEmpty(tokenValue) && config.getCookieAutoFillPrefix()) {
                tokenValue = config.getTokenPrefix() + SaTokenConsts.TOKEN_CONNECTOR_CHAT + tokenValue;
            }
        }

        return tokenValue;
    }


    /**
     * 拼接： 在保存 token - id 映射关系时，应该使用的key
     *
     * @param tokenValue token值
     * @return key
     */
    @Override
    public String splicingKeyTokenValue(String tokenValue) {
        return SecurityConstants.Authentication.TOKEN_SPLICING_KEY_VALUE.apply(loginType) + tokenValue;
    }

    /**
     * 拼接： 在保存 Account-Session 时，应该使用的 key
     *
     * @param loginId 账号id
     * @return key
     */
    @Override
    public String splicingKeySession(Object loginId) {
        return SecurityConstants.Authentication.TOKEN_SPLICING_KEY_SESSION.apply(loginType) + loginId;
    }

    /**
     * 拼接：在保存 Token-Session 时，应该使用的 key
     *
     * @param tokenValue token值
     * @return key
     */
    @Override
    public String splicingKeyTokenSession(String tokenValue) {
        return SecurityConstants.Authentication.TOKEN_SPLICING_KEY_TOKEN_SESSION.apply(loginType) + tokenValue;
    }
}
