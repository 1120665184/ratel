package org.quyq.gwsu.kit.file.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.quyq.gwsu.kit.api.file.enums.FileScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2024/5/19
 * @description 文件信息do ，类似文件超链接 ，多个链接引用同一个文件元信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("kit_file_info")
@Schema(description = "上传文件基本信息模型")
public class KitFileInfo extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "文件ID")
    private String fileId;

    /**
     * 元文件ID
     */
    @Schema(description = "元文件ID")
    private String fileMetaId;

    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String fileName;

    /**
     * 文件大小
     */
    @Schema(description = "文件大小")
    private String fileSize;

    /**
     * 文件后缀
     */
    @Schema(description = "文件后缀")
    private String fileSuffix;

    @Schema(description = "是否为一次性文件（访问一次便删除）")
    private Boolean disposable;


    @Schema(description = "文件过期时间")
    private LocalDateTime expiredTime;

    @Schema(description = "文件作用域")
    private FileScope scope;

    @Schema(description = "作用域为PRIVATE时，配置的允许访问的人员id")
    private String visitors;


    public static KitFileInfo buildByMetaFile(KitFileMetaInfo meta) {
        if (Objects.isNull(meta))
            return null;

        KitFileInfo fileInfo = new KitFileInfo();
        fileInfo.setFileMetaId(meta.getFileMetaId());
        fileInfo.setFileSize(meta.getFileSize());
        return fileInfo;

    }


    public static KitFileInfoVO buildVO(KitFileInfo fileInfo) {
        KitFileInfoVO vo = new KitFileInfoVO();
        vo.setFileId(fileInfo.getFileId());
        vo.setFileMetaId(fileInfo.getFileMetaId());
        vo.setFileName(fileInfo.getFileName());
        vo.setFileSuffix(fileInfo.getFileSuffix());
        vo.setFileSize(fileInfo.getFileSize());
        vo.setVisitors(fileInfo.getVisitors());
        vo.setExpiredTime(fileInfo.getExpiredTime());
        vo.setScope(fileInfo.getScope());
        vo.setDisposable(fileInfo.getDisposable());
        vo.copyBaseProperties(fileInfo);
        return vo;
    }

}
