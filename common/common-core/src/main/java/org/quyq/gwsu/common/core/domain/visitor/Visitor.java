package org.quyq.gwsu.common.core.domain.visitor;


import org.quyq.gwsu.common.core.domain.BaseDO;

/**
 * @author Quyq
 * @date 2026/4/7
 * @description 登录用户基类
 */
//@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public sealed class Visitor extends BaseDO
        permits ClientInfo, UserInfo {
}
