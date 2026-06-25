package org.quyq.gwsu.security.connect.entrance.dingtalk.vo;


import lombok.Data;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description 钉钉用户 staffId映射的真实用户信息
 */
@Data
public class UserStaffIdMappingInfo {

    private String staffId;

    /**
     * 本系统关联的用户ID
     */
    private String subjectId;

    /**
     * 钉钉用户的unionId
     */
    private String unionId;

}
