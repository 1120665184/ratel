package org.quyq.gwsu.kit.api.file.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.kit.api.file.enums.FileScope;
import org.quyq.gwsu.kit.api.file.enums.FileServiceType;

import java.time.LocalDateTime;

/**
 * @author Quyq
 * @date 2026/5/20
 * @description 文件信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class KitFileInfoVO extends BaseVO {


    @TableId(type = IdType.ASSIGN_ID)
    @Schema(title = "文件ID")
    private String fileId;

    /**
     * 元文件ID
     */
    @Schema(title = "元文件ID")
    private String fileMetaId;

    /**
     * 文件名
     */
    @Schema(title = "文件名")
    private String fileName;

    /**
     * 文件大小
     */
    @Schema(title = "文件大小")
    private String fileSize;

    /**
     * 文件后缀
     */
    @Schema(title = "文件后缀")
    private String fileSuffix;

    @Schema(title = "是否为一次性文件（访问一次便删除）")
    private Boolean disposable;


    @Schema(title = "文件过期时间")
    private LocalDateTime expiredTime;

    @Schema(title = "文件作用域")
    private FileScope scope;

    @Schema(title = "作用域为PRIVATE时，配置的允许访问的人员id")
    private String visitors;

    /**
     * 文件md5唯一值
     */
    @Schema(title = "md5唯一值")
    private String uniqueId;

    /**
     * 上传服务类型
     */
    @Schema(title = "上传服务类型")
    private FileServiceType uploadServiceType;

    /**
     * 文件组
     */
    @Schema(title = "组")
    private String fileGroup;

    /**
     * 文件路径
     */
    @Schema(title = "文件路径")
    private String fileUrl;

    /**
     * 文件媒体类型
     */
    @Schema(title = "文件媒体类型")
    private String mediaType;

}
