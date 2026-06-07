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
 * @description 分片上传信息do
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_file_chunk_info")
@Schema(description = "断点续传分片信息模型")
public class KitFileChunkInfo extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "文件分片ID")
    private String fileChunkId;

    /**
     * 文件唯一md5值
     */
    @Schema(description = "文件标识")
    private String uniqueId;

    /**
     * 上传服务类型
     */
    @Schema(description = "上传服务类型")
    private FileServiceType uploadServiceType;

    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String fileName;

    /**
     * 文件媒体类型
     */
    @Schema(description = "文件媒体类型")
    private String mediaType;

    /**
     * 分片偏移量
     */
    @Schema(description = "chunk偏移量")
    private Integer chunkOffset;

    /**
     * 分片流大小
     */
    @Schema(description = "chunk流大小")
    private Integer chunkStreamSize;

    /**
     * 分片组
     */
    @Schema(description = "chunk组")
    private String chunkGroup;

    /**
     * 分片上传路径
     */
    @Schema(description = "chunk上传路径")
    private String chunkUrl;

    /**
     * 到期时长（秒）
     */
    @Schema(description = "到期时长（秒）")
    private Integer expiry;

    /**
     * 上传ID
     */
    @Schema(description = "唯一上传id")
    private String uploadId;


    @Schema(description = "其他信息，用于扩展")
    private String notes;

}
