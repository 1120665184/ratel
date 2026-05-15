package org.quyq.gwsu.security.tablemodel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelForeignKeyVO;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelForeignKey;

import java.util.List;

/**
 * 外键约束信息 服务接口
 *
 * @author Quyq
 */
public interface ISecurityTableModelForeignKeyService extends IService<SecurityTableModelForeignKey> {

    /**
     * 根据ID查询外键信息
     *
     * @param id 主键ID
     * @return 外键信息
     */
    TableModelForeignKeyVO getById(String id);

    /**
     * 根据表ID查询外键列表
     *
     * @param tableId 表ID
     * @return 外键列表
     */
    List<TableModelForeignKeyVO> listByTableId(String tableId);

    /**
     * 保存或更新外键信息
     *
     * @param vo 外键信息VO
     * @return 是否成功
     */
    Boolean saveOrUpdateForeignKey(TableModelForeignKeyVO vo);

    /**
     * 批量保存外键
     *
     * @param foreignKeys 外键列表
     * @return 是否成功
     */
    Boolean saveBatchForeignKeys(List<TableModelForeignKeyVO> foreignKeys);

    /**
     * 根据表ID删除外键
     *
     * @param tableId 表ID
     * @return 是否成功
     */
    Boolean removeByTableId(String tableId);

    /**
     * 批量删除外键
     *
     * @param ids 主键ID列表
     * @return 是否成功
     */
    Boolean removeByIds(List<String> ids);
}
