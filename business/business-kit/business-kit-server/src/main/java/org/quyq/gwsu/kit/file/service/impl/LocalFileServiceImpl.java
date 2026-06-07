package org.quyq.gwsu.kit.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.file.dto.ChunkMultipartDTO;
import org.quyq.gwsu.kit.api.file.enums.FileServiceType;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.config.properties.FileUploadInfoProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.file.domain.KitFileChunkInfo;
import org.quyq.gwsu.kit.file.domain.KitFileMetaInfo;
import org.quyq.gwsu.kit.utils.FileClearUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Quyq
 * @date 2026/5/23
 * @description
 */
@Slf4j
public class LocalFileServiceImpl extends AbstractFileService {
    public LocalFileServiceImpl(FileUploadInfoProperties uploadProperties) {
        super(FileServiceType.LOCAL, uploadProperties.getGroup());
        this.properties = uploadProperties;
        startDeleteBurst();
    }

    private final FileUploadInfoProperties properties;

    //分片目录
    private static final String BURST_DIR = "burst";

    private final Timer timer = new Timer();


    @Override
    void toUpload(KitFileMetaInfo metaInfo, MultipartFile file) {
        String rootPath = getRootPath();
        String nGroup = metaInfo.getFileGroup();
        String url = metaInfo.getFileUrl().replace("/", File.separator);
        if (StringUtils.hasText(nGroup)) {
            url = nGroup + File.separator + url;
        }
        File targetFile = new File(rootPath + url);
        FileUtil.mkParentDirs(targetFile);
        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new BusinessException(KitErrorCode.E01002, e);
        }
    }

    @Override
    byte[] toDownloadBytes(KitFileInfoVO file) throws IOException {
        File target = new File(getRootPath() + file.getFileGroup() + File.separator + file.getFileUrl());
        return Files.readAllBytes(target.toPath());
    }

    @Override
    byte[] toDownloadBytes(KitFileInfoVO file, long startIndex, long endIndex) throws IOException {
        File target = new File(getRootPath() + file.getFileGroup() + File.separator + file.getFileUrl());
        long contentLength = endIndex - startIndex + 1;

        try (RandomAccessFile raf = new RandomAccessFile(target, "r")) {
            raf.seek(startIndex);
            byte[] data = new byte[(int) contentLength];
            raf.readFully(data);
            return data;
        }
    }

    @Override
    void toDelete(KitFileInfoVO file) {
        File target = new File(getRootPath() + file.getFileGroup() + File.separator + file.getFileUrl());
        FileUtil.del(target);
    }

    @Override
    boolean fileIsExist(String group, String fileUrl) {
        return FileUtil.exist(getRootPath() + group + File.separator + fileUrl);
    }


    @Override
    protected String getMultipartUploadId(ChunkMultipartDTO form, String url) {
        String uploadId = IdUtil.getSnowflakeNextIdStr();
        FileUtil.mkdir(getBurstPath(group) + uploadId);
        return uploadId;
    }

    @Override
    protected void completeCancelCallback(String group, String uploadId, String chunkUrl) {
        FileUtil.del(getBurstPath(group) + uploadId);
    }

    @Override
    protected void doCompleteMultipart(List<KitFileChunkInfo> chunkInfos) {
        Optional<KitFileChunkInfo> infoOpt = chunkInfos.stream().findFirst();
        if (infoOpt.isEmpty()) {
            return;
        }
        String uploadId = infoOpt.get().getUploadId();
        String url = infoOpt.get().getChunkUrl();
        String mGroup = infoOpt.get().getChunkGroup();
        File burst = new File(getBurstPath(mGroup) + uploadId);
        File[] burstFiles = burst.listFiles();
        if (!burst.exists() || ArrayUtil.isEmpty(burstFiles)) {
            throw new BusinessException(KitErrorCode.E01004, KitErrorCode.E01002.msg());
        }
        List<File> finBurstFiles = Stream.of(burstFiles).filter(File::isFile)
                .sorted(Comparator.comparing(v -> Integer.parseInt(v.getName())))
                .toList();

        File target = new File(getRootPath() + mGroup + File.separator + url);
        FileUtil.mkParentDirs(target);

        try (FileOutputStream output = new FileOutputStream(target)) {
            for (File b : finBurstFiles) {
                appendStream(b, output);
            }
        } catch (IOException e) {
            throw new BusinessException(KitErrorCode.E01005, e);
        }
        FileUtil.del(burst);
    }


    private void appendStream(File b, FileOutputStream output) throws IOException {
        try (FileInputStream input = IoUtil.toStream(b)) {
            byte[] bytes = new byte[8192];
            int length;
            while ((length = input.read(bytes)) > 0) {
                output.write(bytes, 0, length);
            }
        }
    }

    @Override
    protected void doMultipartChunk(List<KitFileChunkInfo> infos, List<Integer> result) {
        Optional<KitFileChunkInfo> infoOpt = infos.stream().findFirst();
        if (infoOpt.isEmpty()) {
            return;
        }
        File burst = new File(getBurstPath(infoOpt.get().getChunkGroup()) + infoOpt.get().getUploadId());
        if (!burst.exists()) {
            return;
        }
        File[] tmp = burst.listFiles();
        if (ArrayUtil.isEmpty(tmp)) {
            return;
        }
        Stream.of(tmp).filter(File::isFile)
                .forEach(f -> {
                    Integer index = Integer.valueOf(f.getName());
                    infos.stream().filter(v -> v.getChunkOffset().equals(index)).findFirst()
                            .ifPresent(chunkInfo -> {
                                if (chunkInfo.getChunkStreamSize().longValue() == f.length()) {
                                    result.add(chunkInfo.getChunkOffset());
                                }
                            });

                });
    }

    @Override
    protected void doChunkUpload(KitFileChunkInfo info, ChunkMultipartDTO form) {
        String filename = String.valueOf(info.getChunkOffset());
        if (info.getChunkOffset() < 10) {
            filename = "0" + info.getChunkOffset();
        }
        File target = new File(getBurstPath(info.getChunkGroup()) + info.getUploadId() + File.separator + filename);
        FileUtil.mkParentDirs(target);
        try {
            form.getFile().transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(KitErrorCode.E01006, e);
        }
    }

    private void startDeleteBurst() {
        log.debug("启用定时清除分片过期文件删除程序");
        //每天23点执行
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.HOUR_OF_DAY, 23);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                deleteBurstTmp();
            }
        }, instance.getTime(), 1000 * 60 * 60 * 24L);
    }

    /**
     * 上传根路径
     *
     * @return
     */
    private String getRootPath() {
        String path = properties.getLocal().getPath();
        if(!StringUtils.hasText(path)){
            path = System.getProperty("user.dir") + File.separator + "file";
        }
        if (!path.endsWith("/") && !path.endsWith(File.separator)) {
            path += File.separator;
        }
        return path;
    }

    //获取分片存储根路径
    private String getBurstPath(String group) {
        return getRootPath() + group + File.separator + BURST_DIR + File.separator;
    }

    private void deleteBurstTmp() {
        log.info("开始清除分片过期目录...");

        File rootPath = new File(getRootPath());
        if (!rootPath.exists())
            return;

        File[] groups = rootPath.listFiles();
        if (ArrayUtil.isEmpty(groups))
            return;

        Stream.of(groups).filter(File::isDirectory)
                .forEach(g -> {
                    File burstFile = FileUtil.file(g, BURST_DIR);
                    if (!burstFile.exists() || burstFile.isFile())
                        return;

                    File[] uploadFiles = burstFile.listFiles();
                    if (ArrayUtil.isEmpty(uploadFiles))
                        return;
                    List<File> files = Stream.of(uploadFiles).filter(File::isDirectory).collect(Collectors.toList());
                    Collection<List<File>> values = files.stream().collect(Collectors.groupingBy(i -> files.indexOf(i) / 1000))
                            .values();

                    FileClearUtils.burstAllFileAssert(values, chunkInfoMapper, serviceType);

                });


    }

}
