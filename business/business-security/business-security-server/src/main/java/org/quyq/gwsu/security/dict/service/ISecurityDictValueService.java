package org.quyq.gwsu.security.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.api.dict.dto.DictValueSaveDTO;
import org.quyq.gwsu.security.api.dict.vo.DictValueVO;
import org.quyq.gwsu.security.dict.domain.SecurityDictValue;

import java.util.List;

/**
 * 字典值服务接口
 *
 * @author Quyq
 */
public interface ISecurityDictValueService extends IService<SecurityDictValue> {

    /**
     * 查询字典下的值列表
     *
     * @param dictId 字典ID
     * @return 字典值列表
     */
    List<DictValueVO> listByDictId(String dictId);

    /**
     * 新增或更新字典值
     *
     * @param dto 字典值保存请求
     * @return 是否成功
     */
    Boolean saveOrUpdateValue(DictValueSaveDTO dto);

    /**
     * 批量删除字典值
     *
     * @param ids 字典值ID列表
     * @return 是否成功
     */
    Boolean removeByIds(List<String> ids);

    /**
     * 更新排序
     *
     * @param ids 按顺序排列的字典值ID列表
     * @return 是否成功
     */
    Boolean updateSort(List<String> ids);
}
