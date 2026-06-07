package org.quyq.gwsu.kit.file.service.impl;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.apache.tika.mime.MediaType;
import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.kit.api.file.dto.ChunkMultipartDTO;
import org.quyq.gwsu.kit.api.file.dto.FileUploadDTO;
import org.quyq.gwsu.kit.api.file.dto.KitFileInfoDTO;
import org.quyq.gwsu.kit.api.file.enums.FileScope;
import org.quyq.gwsu.kit.api.file.enums.FileServiceType;
import org.quyq.gwsu.kit.api.file.dto.FileStreamWrapper;
import org.quyq.gwsu.kit.api.utils.MediaUtils;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.file.domain.KitFileChunkInfo;
import org.quyq.gwsu.kit.file.domain.KitFileInfo;
import org.quyq.gwsu.kit.file.domain.KitFileMetaInfo;
import org.quyq.gwsu.kit.file.mapper.KitFileChunkInfoMapper;
import org.quyq.gwsu.kit.file.mapper.KitFileInfoMapper;
import org.quyq.gwsu.kit.file.mapper.KitFileMetaInfoMapper;
import org.quyq.gwsu.kit.file.service.IFileService;
import org.quyq.gwsu.kit.utils.FileInfoIntegrationUtil;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Quyq
 * @date 2024/5/20
 * @description 文件服务实现抽象类
 */
@Slf4j
@CacheConfig(cacheNames = "file-info")
public abstract class AbstractFileService implements IFileService {


    protected AbstractFileService(FileServiceType serviceType, String group) {
        if (!StringUtils.hasText(group)) {
            group = SpringUtils.getBean(ProjectUtils.class).getProjectIdent();
        }
        this.group = group;
        this.serviceType = serviceType;
        this.fileInfoMapper = SpringUtils.getBean(KitFileInfoMapper.class);
        this.metaInfoMapper = SpringUtils.getBean(KitFileMetaInfoMapper.class);
        this.chunkInfoMapper = SpringUtils.getBean(KitFileChunkInfoMapper.class);
        this.securityUtils = SpringUtils.getBean(SecurityUtils.class);
    }


    protected final String group;
    /**
     * 服务类型
     */
    protected final FileServiceType serviceType;

    protected final KitFileInfoMapper fileInfoMapper;

    protected final KitFileMetaInfoMapper metaInfoMapper;

    protected final KitFileChunkInfoMapper chunkInfoMapper;

    protected final SecurityUtils securityUtils;


    @Override
    public FileServiceType getServerType() {
        return serviceType;
    }

    @Override
    public IPage<KitFileInfoVO> pageByCondition(KitFileInfoDTO form) {
        QueryWrapper<KitFileInfoVO> wrapper = new QueryWrapper<>();
        wrapper.eq(StringUtils.hasText(form.getFileGroup()), "fmi.file_group", form.getFileGroup())
                .eq(StringUtils.hasText(form.getFileSuffix()), "fi.file_suffix", form.getFileSuffix())
                .like(StringUtils.hasText(form.getFileName()), "fi.file_name", form.getFileName())
                .orderByDesc("fi.create_time");

        return fileInfoMapper.pageByCondition(Page.of(form.getPageNum(), form.getPageSize()), wrapper);
    }

    @Override
    @Cacheable(key = "#id")
    public KitFileInfoVO getById(String id) {
        return fileInfoMapper.getById(id);
    }

    @Override
    @Transactional
    public KitFileInfo upload(FileUploadDTO form) {
        String uniqueId = createUniqueId(form.getFile());
        KitFileMetaInfo metaInfo = getMetaFileByUniqueId(uniqueId);
        if (Objects.nonNull(metaInfo) && fileIsExist(metaInfo.getFileGroup(), metaInfo.getFileUrl())) {
            //文件存在，返回
            return buildFileInfo(form, metaInfo);
        }

        if (Objects.isNull(metaInfo)) {
            metaInfo = buildMetaFileInfo(form.getFile(), form.getCategorize(), uniqueId);
        }
        KitFileInfo fileInfo = buildFileInfo(form, metaInfo);

        toUpload(metaInfo, form.getFile());
        return fileInfo;
    }

    /**
     * 不同文件服务类型上传实现
     *
     * @param metaInfo
     * @param file
     */
    abstract void toUpload(KitFileMetaInfo metaInfo, MultipartFile file);


    @Override
    public FileStreamWrapper download(String fileId, String range) {
        KitFileInfoVO fileInfo = SpringUtils.getAopProxy(this).getById(fileId);
        permissionAssert(fileInfo);

        if (Objects.isNull(fileInfo) || !fileIsExist(fileInfo.getFileGroup(), fileInfo.getFileUrl())) {
            throw new BusinessException(KitErrorCode.E01003, "资源未找到");
        }

        long fileSize = Long.parseLong(fileInfo.getFileSize());
        String fullFileName = fileInfo.getFileName() + "." + fileInfo.getFileSuffix();
        boolean disposable = Boolean.TRUE.equals(fileInfo.getDisposable());

        long startIndex = 0;
        long endIndex = fileSize - 1;

        if (StringUtils.hasText(range) && range.contains("bytes=") && range.contains("-")) {
            String rangeValue = range.substring(range.lastIndexOf("=") + 1).trim();
            String[] ranges = rangeValue.split("-");
            if (rangeValue.startsWith("-")) {
                endIndex = Long.parseLong(ranges[1].trim());
            } else if (rangeValue.endsWith("-")) {
                startIndex = Long.parseLong(ranges[0].trim());
            } else {
                startIndex = Long.parseLong(ranges[0].trim());
                endIndex = Long.parseLong(ranges[1].trim());
            }

            try {
                byte[] data = toDownloadBytes(fileInfo, startIndex, endIndex);
                if (endIndex >= fileSize - 1) {
                    removeDisposableFile(fileInfo);
                }
                return FileStreamWrapper.partial(data, fullFileName, fileInfo.getMediaType(),
                        fileSize, startIndex, endIndex, disposable);
            } catch (IOException ex) {
                throw new BusinessException(KitErrorCode.E01003, ex);
            }
        }

        try {
            byte[] data = toDownloadBytes(fileInfo);
            removeDisposableFile(fileInfo);
            return FileStreamWrapper.full(data, fullFileName, fileInfo.getMediaType(),
                    fileSize, disposable);
        } catch (IOException ex) {
            throw new BusinessException(KitErrorCode.E01003, ex);
        }
    }

    /**
     * 文件权限验证
     *
     * @param fileInfo
     */
    private void permissionAssert(KitFileInfoVO fileInfo) {
        Optional<UserInfo> userInfo = securityUtils.userInfo();
        if (Objects.isNull(fileInfo)) {
            if (userInfo.isEmpty()) {
                throwEx(CommonErrorCode.E03001);
            }
            return;
        }
        if (Arrays.asList(FileScope.PROTECTED, FileScope.PRIVATE).contains(fileInfo.getScope()) && userInfo.isEmpty()) {
            throwEx(CommonErrorCode.E03001);
        } else if (FileScope.PRIVATE == fileInfo.getScope()
                && StringUtils.hasText(fileInfo.getVisitors())
                && userInfo.isPresent()
                && !Arrays.asList(fileInfo.getVisitors().split(",")).contains(userInfo.get().getUserId())) {
            throwEx(CommonErrorCode.E03002);
        }

    }

    /**
     * 一次性文件删除
     *
     * @param fileInfo
     */
    private void removeDisposableFile(KitFileInfoVO fileInfo) {
        if (Boolean.FALSE.equals(fileInfo.getDisposable())) {
            return;
        }
        SpringUtils.getAopProxy(this).remove(fileInfo.getFileId());
    }

    private void throwEx(ReturnCode errorCode) {
        throw new BusinessException(errorCode, "鉴权失败");
    }


    /**
     * 不同文件服务类型下载实现
     *
     * @param file
     * @param response
     */
    abstract byte[] toDownloadBytes(KitFileInfoVO file) throws IOException;

    abstract byte[] toDownloadBytes(KitFileInfoVO file, long startIndex, long endIndex) throws IOException;


    @Override
    @Transactional
    @CacheEvict(key = "#fileId")
    public void remove(String fileId) {
        KitFileInfoVO fileInfo = fileInfoMapper.getById(fileId);
        if (Objects.isNull(fileInfo))
            return;
        if (deleteFileById(fileInfo)) {
            toDelete(fileInfo);
        }

    }

    /**
     * 不同文件系统删除实现
     *
     * @param file
     */
    abstract void toDelete(KitFileInfoVO file);


    @Override
    public Map<String, Object> createMultipartUpload(ChunkMultipartDTO form) {
        //  根据文件名创建签名
        Map<String, Object> result = new HashMap<>();
        List<KitFileChunkInfo> chunkInfos = getChunkInfoByUniqueId(form.getUniqueIdentifier());
        if (!CollectionUtils.isEmpty(chunkInfos)) {
            FileInfoIntegrationUtil.addChunkInfoToResult(chunkInfos, result);
            return result;
        }

        String fileUrl = FileInfoIntegrationUtil.generateFileUrl(form.getCategorize(), FileNameUtil.getSuffix(form.getFile().getOriginalFilename()), form.getUniqueIdentifier());
        String uploadId = getMultipartUploadId(form, fileUrl);
        chunkInfos = buildChunkInfos(form, uploadId, fileUrl, null);
        //生成chunkInfo列表处理回调
        multipartUploadCreatedChunkInfoCallback(chunkInfos);
        FileInfoIntegrationUtil.addChunkInfoToResult(chunkInfos, result);
        chunkInfoMapper.insertBatch(chunkInfos);
        return result;
    }

    /**
     * 生成分片信息后的回调
     *
     * @param chunkInfos
     */
    protected void multipartUploadCreatedChunkInfoCallback(List<KitFileChunkInfo> chunkInfos) {
        //ignore
    }

    /**
     * 获取上传ID
     *
     * @param form
     * @param url
     * @return
     */
    protected String getMultipartUploadId(ChunkMultipartDTO form, String url) {
        throw new BusinessException(KitErrorCode.E01001);
    }


    @Override
    @Transactional
    public KitFileInfo completeMultipartUpload(ChunkMultipartDTO form) {
        List<KitFileChunkInfo> chunkInfos = getChunkInfoByUploadId(form.getUploadId(), null);
        if (StringUtils.isEmpty(chunkInfos))
            throw new BusinessException("合并失败");
        KitFileChunkInfo info = chunkInfos.getFirst();
        String mediaType = info.getMediaType();
        String uniqueId = info.getUniqueId();
        String url = info.getChunkUrl();
        String chunkGroup = info.getChunkGroup();
        String fileName = info.getFileName();
        int fileSize = chunkInfos.stream().map(KitFileChunkInfo::getChunkStreamSize).reduce(0, Integer::sum);

        KitFileMetaInfo metaInfo = getMetaFileByUniqueId(uniqueId);
        if (Objects.nonNull(metaInfo) && fileIsExist(metaInfo.getFileGroup(), metaInfo.getFileUrl())) {
            //删除chunk
            removeChunkInfosByUniqueId(uniqueId);
            //取消合并回调
            completeCancelCallback(chunkGroup, form.getUploadId(), url);
            return buildFileInfo(fileName, form, metaInfo);
        }

        //删除chunk
        removeChunkInfosByUniqueId(uniqueId);

        if (Objects.isNull(metaInfo)) {
            metaInfo = buildMetaFileInfo(mediaType, fileSize, url, chunkGroup, uniqueId);
        } else {
            metaInfo.setFileGroup(chunkGroup)
                    .setFileUrl(url)
                    .setUniqueId(uniqueId)
                    .setFileSize(String.valueOf(fileSize));
            metaInfoMapper.updateById(metaInfo);
        }
        KitFileInfo fileInfo = buildFileInfo(fileName, form, metaInfo);

        doCompleteMultipart(chunkInfos);

        return fileInfo;
    }

    protected void doCompleteMultipart(List<KitFileChunkInfo> chunkInfos) {
        throw new BusinessException(KitErrorCode.E01001);
    }

    /**
     * 取消合并回调
     *
     * @param group
     * @param uploadId
     * @param chunkUrl
     */
    protected void completeCancelCallback(String group, String uploadId, String chunkUrl) {
        //NO
    }

    @Override
    public List<Integer> chunkExist(String uploadId) {
        List<KitFileChunkInfo> chunkInfos = getChunkInfoByUploadId(uploadId, null);
        if (ObjectUtils.isEmpty(chunkInfos)) {
            return Collections.emptyList();
        }
        String uniqueId = chunkInfos.iterator().next().getUniqueId();

        KitFileMetaInfo metaInfo = getMetaFileByUniqueId(uniqueId);
        if (Objects.nonNull(metaInfo) && fileIsExist(metaInfo.getFileGroup(), metaInfo.getFileUrl())) {
            //不为空直接返回所有偏移量都上传完成，实现秒传功能。
            return chunkInfos.stream().map(KitFileChunkInfo::getChunkOffset).collect(Collectors.toList());
        }

        List<Integer> finV = new ArrayList<>();
        doMultipartChunk(chunkInfos, finV);
        return finV;
    }

    /**
     * 检验哪些分片已上传
     * 将已经上传完成的分片下标放入result列表中
     *
     * @param result
     */
    protected void doMultipartChunk(List<KitFileChunkInfo> infos, List<Integer> result) {
        throw new BusinessException(KitErrorCode.E01001);
    }


    @Override
    public void uploadChunk(ChunkMultipartDTO form) {
        List<KitFileChunkInfo> chunkInfo = getChunkInfoByUploadId(form.getUploadId(), form.getOffset());
        if (CollectionUtils.isEmpty(chunkInfo))
            return;

        KitFileChunkInfo ci = chunkInfo.getFirst();
        doChunkUpload(ci, form);
        chunkInfoMapper.updateById(ci);
    }

    /**
     * 分片上传
     *
     * @param info
     * @param form
     */
    protected void doChunkUpload(KitFileChunkInfo info, ChunkMultipartDTO form) {
        throw new BusinessException(KitErrorCode.E01001);
    }

    private void removeChunkInfosByUniqueId(String uniqueId) {
        chunkInfoMapper.delete(new LambdaQueryWrapper<KitFileChunkInfo>()
                .eq(KitFileChunkInfo::getUniqueId, uniqueId)
                .eq(KitFileChunkInfo::getUploadServiceType, serviceType));
    }

    private List<KitFileChunkInfo> buildChunkInfos(ChunkMultipartDTO form, String uploadId, String url, Map<String, Object> result) {
        try {

            MediaType mediaType = MediaUtils.getMediaType(form.getFile().getInputStream(), form.getFile().getOriginalFilename());

            List<KitFileChunkInfo> list = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            List<Integer> chunkSizeList = form.parseChunkSize();
            for (int i = 0; i < chunkSizeList.size(); i++) {
                KitFileChunkInfo info = new KitFileChunkInfo();
                info.setFileChunkId(IdUtil.getSnowflakeNextIdStr())
                        .setMediaType(mediaType.toString())
                        .setFileName(form.getFileName())
                        .setUploadServiceType(serviceType)
                        .setUniqueId(form.getUniqueIdentifier())
                        .setChunkOffset(i)
                        .setChunkStreamSize(chunkSizeList.get(i))
                        .setUploadId(uploadId)
                        .setChunkUrl(url)
                        .setChunkGroup(group)
                        .setExpiry(60 * 60 * 24 * 7)
                        .setCreateTime(now);
                list.add(info);
            }
            if (Objects.nonNull(result)) {
                FileInfoIntegrationUtil.addChunkInfoToResult(list, result);
            }

            return list;
        } catch (IOException ex) {
            throw new BusinessException(ex);
        }
    }

    private List<KitFileChunkInfo> getChunkInfoByUniqueId(String uniqueId) {
        LambdaQueryWrapper<KitFileChunkInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KitFileChunkInfo::getUniqueId, uniqueId)
                .eq(KitFileChunkInfo::getUploadServiceType, serviceType);
        List<KitFileChunkInfo> chunkInfos = chunkInfoMapper.selectList(wrapper);
        if (ObjectUtils.isEmpty(chunkInfos)) {
            return Collections.emptyList();
        }
        KitFileChunkInfo first = chunkInfos.getFirst();
        //判断数据是否过期
        Integer expiry = first.getExpiry();
        LocalDateTime createTime = first.getCreateTime();

        //数据过期
        if (createTime.plusSeconds(expiry).isBefore(LocalDateTime.now())) {
            chunkInfoMapper.delete(new LambdaQueryWrapper<KitFileChunkInfo>()
                    .eq(KitFileChunkInfo::getUniqueId, uniqueId)
                    .eq(KitFileChunkInfo::getUploadServiceType, serviceType));
            return Collections.emptyList();
        }
        return chunkInfos;
    }

    private List<KitFileChunkInfo> getChunkInfoByUploadId(String uploadId, Integer offset) {
        LambdaQueryWrapper<KitFileChunkInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KitFileChunkInfo::getUploadId, uploadId)
                .eq(Objects.nonNull(offset), KitFileChunkInfo::getChunkOffset, offset)
                .eq(KitFileChunkInfo::getUploadServiceType, serviceType);
        return chunkInfoMapper.selectList(wrapper);
    }


    /**
     * 判断文件是否存在
     *
     * @param group
     * @param fileUrl
     * @return
     */
    abstract boolean fileIsExist(String group, String fileUrl);


    private boolean deleteFileById(KitFileInfoVO fileInfo) {
        Long count = fileInfoMapper.selectCount(new LambdaQueryWrapper<KitFileInfo>()
                .eq(KitFileInfo::getFileMetaId, fileInfo.getFileMetaId()));
        fileInfoMapper.deleteById(fileInfo.getFileId());
        if (count == 1) {
            metaInfoMapper.deleteById(fileInfo.getFileMetaId());
            return true;
        }
        return false;
    }


    private KitFileMetaInfo buildMetaFileInfo(String mediaType, int fileSize, String url, String group, String uniqueId) {
        KitFileMetaInfo metaInfo = new KitFileMetaInfo();
        metaInfo.setUploadServiceType(serviceType);
        metaInfo.setFileGroup(group);
        metaInfo.setFileSize(String.valueOf(fileSize));
        metaInfo.setUniqueId(uniqueId);
        metaInfo.setFileUrl(url);
        metaInfo.setMediaType(mediaType);
        metaInfoMapper.insert(metaInfo);
        return metaInfo;
    }

    private KitFileMetaInfo buildMetaFileInfo(MultipartFile file, String categorize, String uniqueId) {
        try {
            MediaType mediaType = MediaUtils.getMediaType(file.getInputStream(), file.getOriginalFilename());

            KitFileMetaInfo metaInfo = new KitFileMetaInfo();
            metaInfo.setUploadServiceType(serviceType);
            metaInfo.setFileGroup(group);
            metaInfo.setFileSize(String.valueOf(file.getSize()));
            metaInfo.setUniqueId(uniqueId);
            metaInfo.setMediaType(mediaType.toString());
            metaInfo.setFileUrl(FileInfoIntegrationUtil.generateFileUrl(categorize, FileNameUtil.getSuffix(file.getOriginalFilename()), uniqueId));
            metaInfoMapper.insert(metaInfo);
            return metaInfo;
        } catch (IOException ex) {
            throw new BusinessException(ex);
        }
    }

    private KitFileInfo buildFileInfo(String fileName, FileUploadDTO form, KitFileMetaInfo meta) {
        KitFileInfo fileInfo = KitFileInfo.buildByMetaFile(meta);
        if (Objects.isNull(fileInfo))
            return null;


        fileInfo.setFileSuffix(FileNameUtil.getSuffix(fileName));
        fileInfo.setFileName(fileName.replace(".%s".formatted(fileInfo.getFileSuffix()), ""));
        fileInfo.setDisposable(Optional.ofNullable(form.getDisposable()).orElse(false));
        fileInfo.setScope(Optional.ofNullable(form.getScope()).orElse(FileScope.PROTECTED));
        fileInfo.setVisitors(form.getVisitors());
        fileInfo.setExpiredTime(form.getExpiredTime());
        fileInfoMapper.insert(fileInfo);
        return fileInfo;
    }

    private KitFileInfo buildFileInfo(FileUploadDTO form, KitFileMetaInfo meta) {
        KitFileInfo fileInfo = KitFileInfo.buildByMetaFile(meta);
        if (Objects.isNull(fileInfo))
            return null;

        String originalFilename = form.getFile().getOriginalFilename();
        fileInfo.setFileSuffix(FileNameUtil.getSuffix(originalFilename));
        String fileName = "";
        if (StringUtils.hasText(originalFilename)) {
            fileName = originalFilename.replace(".%s".formatted(fileInfo.getFileSuffix()), ""); //NOSONAR
        }
        fileInfo.setFileName(fileName);
        fileInfo.setDisposable(Optional.ofNullable(form.getDisposable()).orElse(false));
        fileInfo.setScope(Optional.ofNullable(form.getScope()).orElse(FileScope.PROTECTED));
        fileInfo.setVisitors(form.getVisitors());
        fileInfo.setExpiredTime(form.getExpiredTime());
        fileInfoMapper.insert(fileInfo);
        return fileInfo;

    }

    //通过唯一id查找元数据
    private KitFileMetaInfo getMetaFileByUniqueId(String uniqueId) {
        return metaInfoMapper.selectOne(new LambdaQueryWrapper<KitFileMetaInfo>()
                .eq(KitFileMetaInfo::getUniqueId, uniqueId)
                .eq(KitFileMetaInfo::getUploadServiceType, serviceType)
                .eq(KitFileMetaInfo::getDeleted, false));
    }


    /**
     * 生成md5值
     *
     * @param file
     * @return
     */
    protected String createUniqueId(MultipartFile file) {
        try {
            return DigestUtils.md5DigestAsHex(file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

}
