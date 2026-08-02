package org.quyq.gwsu.kit.api.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.kit.api.file.enums.FileScope;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class FileCopyDTO {

    @Schema(title = "源文件ID")
    private String sourceFileId;

    @Schema(title = "复制后的文件名，包含后缀，可为空")
    private String fileName;

    @Schema(title = "是否为一次性文件（下载一次便删除）")
    private Boolean disposable = false;

    @Schema(title = "文件作用域")
    private FileScope scope;

    @Schema(title = "文件过期时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredTime;

    @Schema(title = "作用域为PRIVATE时，配置的允许访问的人员id , 逗号分割")
    private String visitors;
}
