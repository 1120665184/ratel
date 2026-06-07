package org.quyq.gwsu.kit.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.quyq.gwsu.kit.api.file.enums.FileServiceType;
import org.quyq.gwsu.kit.file.domain.KitFileChunkInfo;
import org.quyq.gwsu.kit.file.mapper.KitFileChunkInfoMapper;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FileClearUtils {

    private FileClearUtils(){}

    /**
     * 校验文件是否过期，过期执行删除
     * @param values
     * @param chunkInfoMapper
     * @param serviceType
     */
    public static void burstAllFileAssert(Collection<List<File>> values , KitFileChunkInfoMapper chunkInfoMapper , FileServiceType serviceType){
        for (List<File> targets : values){
            List<String> uploadIds = targets.stream().map(File::getName).collect(Collectors.toList());
            List<KitFileChunkInfo> chunkInfos = chunkInfoMapper.selectList(new LambdaQueryWrapper<KitFileChunkInfo>()
                    .in(KitFileChunkInfo::getUploadId, uploadIds)
                    .eq(KitFileChunkInfo::getUploadServiceType, serviceType));
            if(CollUtil.isEmpty(chunkInfos)){
                removeBurstDir(targets);
                continue;
            }

            Map<String, List<KitFileChunkInfo>> infoMap = chunkInfos.stream().collect(Collectors.groupingBy(KitFileChunkInfo::getUploadId));

            targets.forEach(f ->{
                String uploadId = f.getName();
                Optional<KitFileChunkInfo> infoOpt = Optional.ofNullable(infoMap.get(uploadId))
                        .map(List::getFirst);
                if(infoOpt.isEmpty()){
                    FileUtil.del(f);
                    return;
                }

                KitFileChunkInfo info = infoOpt.get();

                //已过期
                if(info.getCreateTime().plusSeconds(info.getExpiry()).isBefore(LocalDateTime.now())){
                    FileUtil.del(f);
                }


            });

        }
    }

    private static void removeBurstDir(List<File> targets){
        for (File t : targets){
            FileUtil.del(t);
        }
    }

}
