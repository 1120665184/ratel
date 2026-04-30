package org.quyq.gwsu.system.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.system.manager.domain.SysAccount;

public interface ISysAccountService extends IService<SysAccount> {

    SysAccount getByIdentifier(String identityType, String identifier);

    SysAccount getByUserIdAndType(String userId, String identityType);
}
