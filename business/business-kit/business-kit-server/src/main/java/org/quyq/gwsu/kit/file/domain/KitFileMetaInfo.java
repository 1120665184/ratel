package org.quyq.gwsu.kit.file.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.kit.api.file.enums.FileServiceType;

/**
 * @author Quyq
 * @date 2026/5/19
 * @description 元文件信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_file_meta_info")
@Schema(description = "文件元信息模型")
public class KitFileMetaInfo extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "元文件ID")
    private String fileMetaId;

    /**
     * 文件md5唯一值
     */
    @Schema(description = "文件md5唯一值")
    private String uniqueId;

    /**
     * 上传服务类型
     */
    @Schema(description = "上传服务类型")
    private FileServiceType uploadServiceType;

    /**
     * 文件大小
     */
    @Schema(description = "文件大小")
    private String fileSize;

    /**
     * 文件媒体类型
     */
    @Schema(description = "文件媒体类型")
    private String mediaType;

    /**
     * 文件组
     */
    @Schema(description = "文件组")
    private String fileGroup;

    /**
     * 文件路径
     */
    @Schema(description = "文件路径")
    private String fileUrl;

}
