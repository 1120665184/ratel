package org.quyq.gwsu.security.tablemodel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelColumnVO;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelColumn;

import java.util.List;

/**
 * 字段详细信息 服务接口
 *
 * @author Quyq
 */
public interface ISecurityTableModelColumnService extends IService<SecurityTableModelColumn> {

    /**
     * 根据ID查询字段信息
     *
     * @param id 主键ID
     * @return 字段信息
     */
    TableModelColumnVO getById(String id);

    /**
     * 根据表ID查询字段列表
     *
     * @param tableId 表ID
     * @return 字段列表
     */
    List<TableModelColumnVO> listByTableId(String tableId);

    /**
     * 保存或更新字段信息
     *
     * @param vo 字段信息VO
     * @return 是否成功
     */
    Boolean saveOrUpdateColumn(TableModelColumnVO vo);

    /**
     * 批量保存字段
     *
     * @param columns 字段列表
     * @return 是否成功
     */
    Boolean saveBatchColumns(List<TableModelColumnVO> columns);

    /**
     * 根据表ID删除字段
     *
     * @param tableId 表ID
     * @return 是否成功
     */
    Boolean removeByTableId(String tableId);

    /**
     * 批量删除字段
     *
     * @param ids 主键ID列表
     * @return 是否成功
     */
    Boolean removeByIds(List<String> ids);

    /**
     * 更新字段注释
     *
     * @param columnId      字段ID
     * @param columnComment 新注释
     * @return 是否成功
     */
    Boolean updateComment(String columnId, String columnComment);

    Boolean updateDictKey(String columnId, String dictKey);
}
