package org.quyq.gwsu.kit.file.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.file.domain.KitFileInfo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Quyq
 * @date 2024/5/20
 * @description 文件信息
 */
public interface KitFileInfoMapper extends BaseMapper<KitFileInfo> {


    /**
     * 分页查询
     *
     * @param page
     * @param wrapper
     * @return
     */
    IPage<KitFileInfoVO> pageByCondition(IPage<KitFileInfoVO> page, @Param(Constants.WRAPPER) Wrapper<KitFileInfoVO> wrapper);

    /**
     * 查找通过ID
     *
     * @param id
     * @return
     */
    KitFileInfoVO getById(@Param("id") Serializable id);


    /**
     * 获取过期文件列表
     *
     * @return
     */
    List<KitFileInfoVO> getExpiredFilelist(@Param("now") LocalDateTime now, @Param("serverType") String serverType);

}
