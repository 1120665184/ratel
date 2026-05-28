package org.quyq.gwsu.security.dict.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.api.dict.dto.DictQueryDTO;
import org.quyq.gwsu.security.api.dict.dto.DictSaveDTO;
import org.quyq.gwsu.security.api.dict.vo.DictVO;
import org.quyq.gwsu.security.dict.domain.SecurityDict;

import java.util.List;

/**
 * 字典服务接口
 *
 * @author Quyq
 */
public interface ISecurityDictService extends IService<SecurityDict> {

    /**
     * 根据ID查询字典
     *
     * @param id 字典ID
     * @return 字典信息
     */
    DictVO getById(String id);

    /**
     * 分页查询字典
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<DictVO> pageByCondition(DictQueryDTO query);

    /**
     * 新增或更新字典
     *
     * @param dto 字典保存请求
     * @return 是否成功
     */
    Boolean saveOrUpdateDict(DictSaveDTO dto);

    /**
     * 批量删除字典
     *
     * @param ids 字典ID列表
     * @return 是否成功
     */
    Boolean removeByIds(List<String> ids);
}
