package org.quyq.gwsu.kit.file.media;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.mime.MimeTypeException;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.quyq.gwsu.kit.api.utils.MediaUtils;
import org.quyq.gwsu.kit.config.properties.FileExtensionProperties;
import org.quyq.gwsu.kit.config.properties.FileUploadInfoProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.quyq.gwsu.kit.file.service.FileServiceManager.FILE_SYSTEM_CONFIG;

/**
 * @author Quyq
 * @date 2024/5/25
 * @description 允许文件类型
 */
@Component
@Slf4j
public class AllowFileType {

    public final static String File_EXTENSION_CONFIG = "upload_file_extension_config";


    /**
     * 通过文件流获取真实文件后缀
     *
     * @param file
     * @return
     */
    public String getFileExtension(MultipartFile file) {

        try {

            List<String> types = MediaUtils.getSuffixByMediaType(
                    MediaUtils.getMediaType(file.getInputStream(), file.getOriginalFilename())
            );

            if (CollUtil.isEmpty(types)) {
                return null;
            }
            String suffix = FileNameUtil.getSuffix(file.getOriginalFilename());
            if (types.contains(suffix))
                return suffix;

            return types.getFirst();
        } catch (MimeTypeException | IOException e) {
            throw new BusinessException(KitErrorCode.E01009, e);
        }

    }


    /**
     * 验证文件后缀
     *
     * @param file
     */
    public void valid(MultipartFile file) {

        FileExtensionProperties exProperties = ConfigInfoUtils.getByObject(File_EXTENSION_CONFIG, FileExtensionProperties.class);

        if (file.getSize() == 0L) {
            throw new BusinessException(KitErrorCode.E01010, KitErrorCode.E01013.msg());
        }

        String extension = getFileExtension(file);
        if (!StringUtils.hasText(extension)) {
            log.error("文件类型验证失败-未识别出文件类型，禁止文件上传");
            throw new BusinessException(KitErrorCode.E01014, KitErrorCode.E01013.msg());
        }


        String old = FileNameUtil.getSuffix(file.getOriginalFilename());
        //多个.的文件名后缀会取错
        if (StringUtils.hasText(old)) {
            String[] tmp = old.split("\\.");
            old = tmp[tmp.length - 1];

            if (!extension.equalsIgnoreCase(old)) {
                log.error("文件类型验证失败-文件后缀可能被串改，禁止上传。原类型：{} ,检测类型：{}", old, extension);
                throw new BusinessException(KitErrorCode.E01010, KitErrorCode.E01013.msg());
            }
        }


        if (!exProperties.isEnabled()) {
            return;
        }


        List<String> disabled = Arrays.stream(exProperties.getDisable().split(","))
                .map(String::toLowerCase).toList();
        if (disabled.contains(extension.toLowerCase())) {
            throw new BusinessException(KitErrorCode.E01012, KitErrorCode.E01013.msg());
        }


    }


}
