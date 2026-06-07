package org.quyq.gwsu.kit.api.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChunkMultipartDTO extends FileUploadDTO {

    @Schema(title = "文件名")
    private String fileName;

    @Schema(title = "md5唯一加密值")
    private String uniqueIdentifier;

    @Schema(title = "上传ID")
    private String uploadId;

    @Schema(title = "备注")
    private String notes;

    @Schema(title = "文件偏移量")
    private Integer offset;

    @Schema(title = "每个分片的大小（逗号分隔）")
    private String chunkSize;

    public List<Integer> parseChunkSize() {
        if (Objects.isNull(chunkSize) || chunkSize.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(chunkSize.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    public void applyChunkSizeList(List<Integer> chunkSizeList) {
        if (Objects.isNull(chunkSizeList) || chunkSizeList.isEmpty()) {
            this.chunkSize = null;
            return;
        }
        this.chunkSize = chunkSizeList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

}
