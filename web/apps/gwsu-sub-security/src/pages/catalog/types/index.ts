/** Catalog 信息 */
export interface CatalogInfo {
  id?: string;
  catalogKey: string;
  catalogName: string;
  description?: string;
  version?: string;
  active: number;
  status: boolean;
  createTime?: string;
}

/** Catalog 组件信息 */
export interface CatalogComponentInfo {
  id?: string;
  componentName: string;
  description: string;
  propsSchema: string;
  defaultProps?: string;
  category?: string;
  sortOrder?: number;
  status: boolean;
  createTime?: string;
}

/** 组件定义（CatalogDefinitionVO 中的嵌套结构） */
export interface ComponentDefinition {
  componentName: string;
  description: string;
  propsSchema: string;
  defaultProps?: string;
  category?: string;
}

/** Catalog 完整定义 */
export interface CatalogDefinition {
  catalogKey: string;
  catalogName: string;
  components: ComponentDefinition[];
}

/** 组件分类选项 */
export const CATEGORY_OPTIONS = [
  { label: '展示', value: 'display' },
  { label: '图表', value: 'chart' },
  { label: '表单', value: 'form' },
];
