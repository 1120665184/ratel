package org.quyq.gwsu.security.tablemodel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.database.metadata.model.ColumnInfo;
import org.quyq.gwsu.common.database.metadata.model.TableInfo;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelChangeDatasourceDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelCollectDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelCustomSaveDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelTableQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelUncollectedCountDTO;
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

    /**
     * 分页查询表模型列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<TableModelTableVO> pageByCondition(TableModelTableQueryDTO query);

    /**
     * 查询未采集的表模型列表（从api_table_model中分析）
     *
     * @param modulePrefix 模块前缀
     * @return 未采集的表模型列表
     */
    List<TableModelTableVO> listUncollected(String modulePrefix);

    /**
     * 采集表模型（批量保存表+字段+外键）
     *
     * @param dto 采集请求
     * @return 是否成功
     */
    Boolean collectTableModels(TableModelCollectDTO dto);

    /**
     * 自定义添加表模型
     *
     * @param dto 自定义添加请求
     * @return 表模型信息
     */
    TableModelTableVO customSave(TableModelCustomSaveDTO dto);

    /**
     * 同步表模型字段（与库中最新对比，增删字段）
     *
     * @param tableModelId    表模型ID
     * @param applicationName 服务名
     * @return 是否成功
     */
    Boolean syncTableModel(String tableModelId, String applicationName);

    /**
     * 修改数据源
     *
     * @param dto 修改数据源请求
     * @return 是否成功
     */
    Boolean changeDatasource(TableModelChangeDatasourceDTO dto);

    /**
     * 更新表注释
     *
     * @param tableId      表模型ID
     * @param tableComment 新注释
     * @return 是否成功
     */
    Boolean updateTableComment(String tableId, String tableComment);

    /**
     * 统计各模块未采集表模型数量
     *
     * @param dto 模块列表（含 modulePrefix 和 applicationName）
     * @return modulePrefix → 未采集数量
     */
    java.util.Map<String, Integer> uncollectedCount(TableModelUncollectedCountDTO dto);

}
