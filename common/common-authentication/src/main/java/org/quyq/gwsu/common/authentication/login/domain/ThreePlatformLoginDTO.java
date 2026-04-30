package org.quyq.gwsu.common.authentication.login.domain;


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
public class ThreePlatformLoginDTO extends AbstractLoginDTO {

    /**
     * 返回码
     */
    private String code;

}
