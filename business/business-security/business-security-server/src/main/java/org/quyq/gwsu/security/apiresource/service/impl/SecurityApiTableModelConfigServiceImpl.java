package org.quyq.gwsu.security.apiresource.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiTableModelConfig;
import org.quyq.gwsu.security.apiresource.mapper.SecurityApiTableModelConfigMapper;
import org.quyq.gwsu.security.apiresource.service.ISecurityApiTableModelConfigService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityApiTableModelConfigServiceImpl extends ServiceImpl<SecurityApiTableModelConfigMapper, SecurityApiTableModelConfig>
        implements ISecurityApiTableModelConfigService {
}
