package org.quyq.gwsu.system.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.system.api.manager.vo.DingTalkAccountOptionVO;
import org.quyq.gwsu.system.manager.domain.SysAccount;
import org.quyq.gwsu.system.manager.domain.SysUser;
import org.quyq.gwsu.system.manager.mapper.SysAccountMapper;
import org.quyq.gwsu.system.manager.mapper.SysUserMapper;
import org.quyq.gwsu.system.manager.service.ISysAccountService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysAccountServiceImpl extends ServiceImpl<SysAccountMapper, SysAccount> implements ISysAccountService {

    private final SysUserMapper userMapper;

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

    @Override
    public List<DingTalkAccountOptionVO> listBindableDingTalkAccounts() {
        // 1. 查询所有 dingtalk 类型的账号
        List<SysAccount> dingTalkAccounts = list(new LambdaQueryWrapper<SysAccount>()
                .eq(SysAccount::getIdentityType, "dingtalk"));

        if (dingTalkAccounts.isEmpty()) {
            return List.of();
        }

        // 2. 找出只有 dingtalk 一种登录方式的用户（即未绑定其他登录方式）
        Set<String> dingTalkUserIds = dingTalkAccounts.stream()
                .map(SysAccount::getUserId)
                .collect(Collectors.toSet());

        // 查询这些用户的所有非 dingtalk 账号
        List<SysAccount> otherAccounts = list(new LambdaQueryWrapper<SysAccount>()
                .in(SysAccount::getUserId, dingTalkUserIds)
                .ne(SysAccount::getIdentityType, "dingtalk"));

        // 有其他登录方式的用户ID集合
        Set<String> usersWithOtherLogin = otherAccounts.stream()
                .map(SysAccount::getUserId)
                .collect(Collectors.toSet());

        // 过滤：只保留仅有 dingtalk 登录方式的用户
        List<SysAccount> bindableAccounts = dingTalkAccounts.stream()
                .filter(a -> !usersWithOtherLogin.contains(a.getUserId()))
                .toList();

        if (bindableAccounts.isEmpty()) {
            return List.of();
        }

        // 3. 查询对应用户信息并组装 VO
        Set<String> bindableUserIds = bindableAccounts.stream()
                .map(SysAccount::getUserId)
                .collect(Collectors.toSet());

        List<SysUser> users = userMapper.selectByIds(bindableUserIds);
        var userMap = users.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<DingTalkAccountOptionVO> result = new ArrayList<>();
        for (SysAccount account : bindableAccounts) {
            SysUser user = userMap.get(account.getUserId());
            if (user == null) {
                continue;
            }
            DingTalkAccountOptionVO vo = new DingTalkAccountOptionVO();
            vo.setId(account.getId());
            vo.setIdentifier(account.getIdentifier());
            vo.setUserId(user.getId());
            vo.setNickname(user.getNickname());
            vo.setUserName(user.getUsername());
            result.add(vo);
        }

        return result;
    }
}
