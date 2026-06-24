package org.quyq.gwsu.system.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.system.api.manager.vo.DingTalkAccountOptionVO;
import org.quyq.gwsu.system.manager.domain.SysAccount;

import java.util.List;

public interface ISysAccountService extends IService<SysAccount> {

    SysAccount getByIdentifier(String identityType, String identifier);

    SysAccount getByUserIdAndType(String userId, String identityType);

    /**
     * 获取可绑定的钉钉账号列表
     * 条件：identity_type = dingtalk 且该用户只有 dingtalk 一种登录方式（未绑定其他登录方式）
     *
     * @return 可绑定的钉钉账号选项列表
     */
    List<DingTalkAccountOptionVO> listBindableDingTalkAccounts();
}
