package org.quyq.gwsu.system.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.system.manager.domain.SysAccount;
import org.quyq.gwsu.system.manager.mapper.SysAccountMapper;
import org.quyq.gwsu.system.manager.service.ISysAccountService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysAccountServiceImpl extends ServiceImpl<SysAccountMapper, SysAccount> implements ISysAccountService {

    @Override
    public SysAccount getByIdentifier(String identityType, String identifier) {
        return getOne(new LambdaQueryWrapper<SysAccount>()
                .eq(SysAccount::getIdentityType, identityType)
                .eq(SysAccount::getIdentifier, identifier));
    }

    @Override
    public SysAccount getByUserIdAndType(String userId, String identityType) {
        return getOne(new LambdaQueryWrapper<SysAccount>()
                .eq(SysAccount::getUserId, userId)
                .eq(SysAccount::getIdentityType, identityType));
    }
}
