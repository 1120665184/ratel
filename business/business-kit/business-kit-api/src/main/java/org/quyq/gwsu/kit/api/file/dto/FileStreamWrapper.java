package org.quyq.gwsu.kit.api.file.dto;

import lombok.Data;

@Data
public class FileStreamWrapper {

    private byte[] data;

    private String fileName;

    private String mediaType;

    private long fileSize;

    private long startIndex;

    private long endIndex;

    private boolean partial;

    private boolean disposable;

    public static FileStreamWrapper full(byte[] data, String fileName, String mediaType, long fileSize, boolean disposable) {
        FileStreamWrapper wrapper = new FileStreamWrapper();
        wrapper.setData(data);
        wrapper.setFileName(fileName);
        wrapper.setMediaType(mediaType);
        wrapper.setFileSize(fileSize);
        wrapper.setStartIndex(0);
        wrapper.setEndIndex(fileSize - 1);
        wrapper.setPartial(false);
        wrapper.setDisposable(disposable);
        return wrapper;
    }

    public static FileStreamWrapper partial(byte[] data, String fileName, String mediaType, long fileSize,
                                            long startIndex, long endIndex, boolean disposable) {
        FileStreamWrapper wrapper = new FileStreamWrapper();
        wrapper.setData(data);
        wrapper.setFileName(fileName);
        wrapper.setMediaType(mediaType);
        wrapper.setFileSize(fileSize);
        wrapper.setStartIndex(startIndex);
        wrapper.setEndIndex(endIndex);
        wrapper.setPartial(true);
        wrapper.setDisposable(disposable);
        return wrapper;
    }

}
