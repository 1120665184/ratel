package org.quyq.gwsu.kit.config.properties;


import lombok.Data;

/**
 * @author Quyq
 * @date 2026/6/7
 * @description
 */
@Data
public class FileExtensionProperties {

    /**
     * 启用后缀检测
     */
    private boolean enabled = false;

    /**
     * 禁止的后缀
     */
    private String disable;

}
