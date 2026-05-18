import { get, post } from '@gwsu/core';
import type {
  TableModelQuery,
  TableModelPageResult,
  TableModelDetail,
  TableModelInfo,
  ModuleInfo,
  ApiResourceSimple,
  CollectItem,
  ChangeDatasourceRequest,
} from '../types';

/** 分页查询表模型列表 */
export async function getTableModelPage(query: TableModelQuery): Promise<TableModelPageResult> {
  const res = await post<TableModelPageResult>('/security/tablemodel/page', query);
  return res.data;
}

/** 查询未采集的表模型列表 */
export async function listUncollected(modulePrefix: string): Promise<TableModelInfo[]> {
  const res = await post<TableModelInfo[]>('/security/tablemodel/listUncollected', { modulePrefix });
  return res.data;
}

/** 采集表模型 */
export async function collectTableModels(items: CollectItem[]): Promise<boolean> {
  const res = await post<boolean>('/security/tablemodel/collect', { items });
  return res.data;
}

/** 自定义添加表模型 */
export async function customSaveTableModel(data: {
  applicationName: string;
  modulePrefix: string;
  datasource: string;
  tableName: string;
}): Promise<TableModelInfo> {
  const res = await post<TableModelInfo>('/security/tablemodel/customSave', data);
  return res.data;
}

/** 同步表模型字段 */
export async function syncTableModel(tableModelId: string): Promise<boolean> {
  const res = await post<boolean>(`/security/tablemodel/sync/${tableModelId}`);
  return res.data;
}

/** 修改数据源 */
export async function changeDatasource(data: ChangeDatasourceRequest): Promise<boolean> {
  const res = await post<boolean>('/security/tablemodel/changeDatasource', data);
  return res.data;
}

/** 获取表模型详情 */
export async function getTableModelDetail(
  modulePrefix: string,
  datasource: string,
  tableName: string,
): Promise<TableModelDetail> {
  const res = await get<TableModelDetail>('/security/tablemodel/detail', {
    modulePrefix,
    datasource,
    tableName,
  });
  return res.data;
}

/** 获取模块列表 */
export async function getModuleList(): Promise<ModuleInfo[]> {
  const res = await post<ModuleInfo[]>('/modules/list');
  return res.data;
}

/** 通过服务名获取数据源列表 */
export async function getDatasourceList(applicationName: string): Promise<string[]> {
  const res = await post<string[]>('/security/apiResource/getDatasourceList', { applicationName });
  return res.data;
}

/** 获取指定表模型关联的所有接口资源 */
export async function listApiByTableModel(data: {
  modulePrefix: string;
  datasource: string;
  tableName: string;
}): Promise<ApiResourceSimple[]> {
  const res = await post<ApiResourceSimple[]>('/security/apiResource/listByTableModel', data);
  return res.data;
}

/** 查询指定数据源的表列表 */
export async function getTableList(applicationName: string, datasource?: string): Promise<string[]> {
  const res = await post<string[]>('/security/tablemodel/table/info', { applicationName, datasource });
  return res.data;
}

/** 更新字段注释 */
export async function updateColumnComment(columnId: string, columnComment: string): Promise<boolean> {
  const res = await post<boolean>('/security/tablemodel/column/updateComment', { columnId, columnComment });
  return res.data;
}

/** 更新外键备注 */
export async function updateForeignKeyRemark(fkId: string, remark: string): Promise<boolean> {
  const res = await post<boolean>('/security/tablemodel/foreignKey/updateRemark', { fkId, remark });
  return res.data;
}

/** 保存外键（新增/更新） */
export async function saveForeignKey(data: Record<string, unknown>): Promise<boolean> {
  const res = await post<boolean>('/security/tablemodel/foreignKey/save', data);
  return res.data;
}
