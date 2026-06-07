package org.quyq.gwsu.kit.api.utils;

import cn.hutool.core.collection.CollUtil;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Quyq
 * @date 2024/5/25
 * @description 文件媒体类型工具类
 */

public class MediaUtils {

    private MediaUtils(){}


    /**
     * 得到文件媒体类型
     * @param stream
     * @param filename
     * @return
     * @throws IOException
     */
    public static MediaType getMediaType(InputStream stream ,String filename) throws IOException {
        TikaInputStream tStream = TikaInputStream.get(stream);

        Metadata metadata = new Metadata();
        metadata.add(TikaCoreProperties.RESOURCE_NAME_KEY ,filename);
        return TikaConfig.getDefaultConfig().getDetector().detect(tStream,metadata );
    }


    /**
     * 通过媒体类型获取文件后缀
     * @param type
     * @return
     * @throws MimeTypeException
     */
    public static List<String> getSuffixByMediaType(MediaType type) throws MimeTypeException {
        List<String> extension = MimeTypes.getDefaultMimeTypes().forName(type.toString()).getExtensions();
        if(CollUtil.isEmpty(extension)){
            return Collections.emptyList();
        }
        return extension.stream().map(v->v.replace(".","")).collect(Collectors.toList());
    }

}
