package org.quyq.gwsu.common.authentication.login.impl.headless;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.authentication.domain.AbstractLoginDTO;

/**
 * @author Quyq
 * @date 2026/6/13
 * @description 专门用于无头浏览器快速登录
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HeadlessLoginDTO extends AbstractLoginDTO {

    public static final String LOGIN_TYPE = "headless";

    private String certificationKey;

}
