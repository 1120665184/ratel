package org.quyq.gwsu.kit.api.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.kit.api.file.enums.FileScope;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * @author Quyq
 * @date 2024/11/14
 * @description
 */
@Data
public class FileUploadDTO {

    /**
     * 文件
     */
    @Schema(title = "上传文件")
    private MultipartFile file;

    @Schema(title = "是否为一次性文件（下载一次便删除）")
    private Boolean disposable = false;

    @Schema(title = "文件作用域")
    private FileScope scope;

    @Schema(title = "文件过期时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredTime;

    @Schema(title = "作用域为PRIVATE时，配置的允许访问的人员id , 逗号分割")
    private String visitors;

    @Schema(title = "文件类别")
    private String categorize;

}
