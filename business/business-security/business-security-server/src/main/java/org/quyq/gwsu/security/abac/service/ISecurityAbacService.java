package org.quyq.gwsu.security.abac.service;

import org.quyq.gwsu.security.abac.domain.SecurityAbac;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ISecurityAbacService extends IService<SecurityAbac> {

    /**
     * 同步接口访问策略
     */
    void syncPolicies();

    /**
     * 同步字段访问策略
     */
    void syncFieldPolicies();

}
