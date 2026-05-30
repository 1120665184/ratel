package org.quyq.gwsu.security.dict.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.api.dict.dto.DictQueryDTO;
import org.quyq.gwsu.security.api.dict.dto.DictSaveDTO;
import org.quyq.gwsu.security.api.dict.vo.DictVO;
import org.quyq.gwsu.common.security.api.vo.DictValueVO;
import org.quyq.gwsu.security.dict.domain.SecurityDict;
import org.quyq.gwsu.security.dict.domain.SecurityDictValue;

import java.util.List;

/**
 * 字典服务接口
 *
 * @author Quyq
 */
public interface ISecurityDictService extends IService<SecurityDict> {

    String DICT_DATA_CACHE_PREFIX = "dict_data";

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
     * 通过字典标签key获取字典值
     * @param dictKey
     * @return
     */
    List<DictValueVO> getByDictKey(String dictKey);


    /**
     * 保存或更新字典值
     * @param dto
     * @return
     */
    Boolean saveOrUpdateDictValue(SecurityDictValue dto);

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

    /**
     * 通过ID批量删除字典值
     * @param ids
     * @return
     */
    Boolean removeDictValueByIds(List<String> ids);

    /**
     * 通过字典key删除字典值
     *
     * @param dictKey
     */
    void removeDictValueByKey(String dictKey);

    /**
     * 更新字典值的顺序
     * @param dictKey
     * @param ids
     * @return
     */
    Boolean updateDictValueSort(String dictKey , List<String> ids);
}
