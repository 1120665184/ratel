package org.quyq.gwsu.security.tablemodel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.database.metadata.model.ColumnInfo;
import org.quyq.gwsu.common.database.metadata.model.TableInfo;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelDetailVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelTableVO;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelTable;

import java.util.List;

/**
 * 表基本信息 服务接口
 *
 * @author Quyq
 */
public interface ISecurityTableModelTableService extends IService<SecurityTableModelTable> {

    /**
     * 根据ID查询表信息
     *
     * @param id 主键ID
     * @return 表信息
     */
    TableModelTableVO getById(String id);


    /**
     * 获取指定表的详细内容，包含字段等
     * @param modulePrefix
     * @param datasource
     * @param tableName
     * @return
     */
    TableModelDetailVO getTableDetail(String modulePrefix , String datasource , String tableName);

    /**
     * 根据表名和数据源查询
     *
     * @param tableName  表名
     * @param dataSource 数据源
     * @return 表信息
     */
    TableModelTableVO getByTableNameAndDataSource(String tableName, String dataSource);

    /**
     * 查询表信息列表
     *
     * @return 表信息列表
     */
    List<TableModelTableVO> listAll();

    /**
     * 保存或更新表信息
     *
     * @param vo 表信息VO
     * @return 是否成功
     */
    Boolean saveOrUpdateTable(TableModelTableVO vo);

    /**
     * 批量删除表信息
     *
     * @param ids 主键ID列表
     * @return 是否成功
     */
    Boolean removeByIds(List<String> ids);

    /**
     * 获取表信息
     * @param applicationName 服务名
     * @param datasource
     * @return
     */
    List<TableInfo> tableList(String applicationName ,String datasource);

    /**
     * 获取表的列信息
     * @param applicationName 服务名
     * @param datasource
     * @param tableName
     * @return
     */
    List<ColumnInfo>  columnList(String applicationName ,String datasource, String tableName);

}
