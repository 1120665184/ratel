package org.quyq.gwsu.common.authentication.login.impl.password;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.authentication.domain.AbstractLoginDTO;

/**
 * @author Quyq
 * @date 2026/4/8
 * @description
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PasswordLoginDTO extends AbstractLoginDTO {

    public static final String LOGIN_TYPE = "password";

    private String username;

    private String password;


}
