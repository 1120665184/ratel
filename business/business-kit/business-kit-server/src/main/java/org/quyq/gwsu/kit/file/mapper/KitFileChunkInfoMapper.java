package org.quyq.gwsu.kit.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.file.domain.KitFileChunkInfo;

import java.util.List;

/**
 * @author Quyq
 * @date 2024/5/20
 * @description 文件分片上传mapper
 */
public interface KitFileChunkInfoMapper extends BaseMapper<KitFileChunkInfo> {

    /**
     * 批量插入
     *
     * @param chunks
     */
    void insertBatch(@Param("chunks") List<KitFileChunkInfo> chunks);

}
