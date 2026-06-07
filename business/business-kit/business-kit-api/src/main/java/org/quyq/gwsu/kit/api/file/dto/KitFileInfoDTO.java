package org.quyq.gwsu.kit.api.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

/**
 * @author Quyq
 * @date 2026/5/20
 * @description 文件信息查询对象
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class KitFileInfoDTO extends BaseDTO {

    /**
     * 文件名
     */
    @Schema(title = "文件名")
    private String fileName;

    /**
     * 文件后缀
     */
    @Schema(title = "文件后缀")
    private String fileSuffix;

    /**
     * 文件组
     */
    @Schema(title = "文件组")
    private String fileGroup;

}
