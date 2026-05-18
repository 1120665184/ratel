/** 表模型信息 */
export interface TableModelInfo {
  id: string;
  tableName: string;
  modulePrefix: string;
  dataSource: string;
  tableComment: string;
  sourceType: number;
  createTime?: string;
  modifyTime?: string;
}

/** 表模型详情（表+字段+外键） */
export interface TableModelDetail {
  table: TableModelInfo;
  columns: TableModelColumnInfo[];
  foreignKeys: TableModelForeignKeyInfo[];
}

/** 字段信息 */
export interface TableModelColumnInfo {
  id: string;
  tableId: string;
  columnName: string;
  columnType: string;
  columnLength: number | null;
  columnScale: number | null;
  isNullable: boolean;
  isPrimaryKey: boolean;
  pkPosition: number | null;
  defaultValue: string | null;
  columnComment: string | null;
  ordinalPosition: number;
}

/** 外键信息 */
export interface TableModelForeignKeyInfo {
  id: string;
  constraintName: string;
  tableId: string;
  columnName: string;
  referencedTableName: string;
  referencedColumnName: string;
  dataType: number;
  remark: string | null;
  updateRule: string;
  deleteRule: string;
}

/** 表模型分页查询条件 */
export interface TableModelQuery {
  modulePrefix?: string;
  tableName?: string;
  dataSource?: string;
  sourceType?: number;
  pageNum: number;
  pageSize: number;
}

/** 分页结果 */
export interface TableModelPageResult {
  records: TableModelInfo[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

/** 模块信息 */
export interface ModuleInfo {
  prefix: string;
  applicationName: string;
  note: string;
}

/** 接口资源信息（简化） */
export interface ApiResourceSimple {
  id: string;
  modulePrefix: string;
  tagName: string;
  reqPath: string;
  reqMethod: string;
  summary: string;
}

/** 采集请求项 */
export interface CollectItem {
  modulePrefix: string;
  datasource: string;
  tableName: string;
}

/** 修改数据源请求 */
export interface ChangeDatasourceRequest {
  tableModelId: string;
  newDatasource: string;
  apiIds?: string[];
}

/** 来源类型映射 */
export const SOURCE_TYPE_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '采集', color: 'blue' },
  1: { text: '自定义', color: 'green' },
};
