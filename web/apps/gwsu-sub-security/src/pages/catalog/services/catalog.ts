import { get, post, put, del } from '@gwsu/core';
import type {
  CatalogInfo,
  CatalogComponentInfo,
  CatalogDefinition,
} from '../types';

const BASE = '/security/catalog';
const COMP_BASE = '/security/catalog/component';

// =================== Catalog 接口 ===================

/** 查询Catalog列表 */
export async function getCatalogList(): Promise<CatalogInfo[]> {
  const res = await get<CatalogInfo[]>(`${BASE}/list`);
  return res.data ?? [];
}

/** 根据ID查询Catalog */
export async function getCatalogById(id: string): Promise<CatalogInfo> {
  const res = await get<CatalogInfo>(`${BASE}/${id}`);
  return res.data;
}

/** 新增或更新Catalog */
export async function saveOrUpdateCatalog(data: CatalogInfo): Promise<string> {
  const res = await post<string>(BASE, data);
  return res.data;
}

/** 批量删除Catalog */
export async function deleteCatalogs(ids: string[]): Promise<boolean> {
  const res = await del<boolean>(BASE, ids);
  return res.data;
}

/** 激活Catalog */
export async function activateCatalog(id: string): Promise<boolean> {
  const res = await put<boolean>(`${BASE}/activate/${id}`);
  return res.data;
}

/** 获取当前激活的Catalog完整定义 */
export async function getActiveCatalogDefinition(): Promise<CatalogDefinition> {
  const res = await get<CatalogDefinition>(`${BASE}/active-definition`);
  return res.data;
}

/** 根据catalogKey获取Catalog完整定义 */
export async function getCatalogDefinitionByKey(catalogKey: string): Promise<CatalogDefinition> {
  const res = await get<CatalogDefinition>(`${BASE}/definition/${catalogKey}`);
  return res.data;
}

/** 给Catalog绑定组件列表 */
export async function bindComponents(catalogId: string, componentIds: string[]): Promise<boolean> {
  const res = await put<boolean>(`${BASE}/${catalogId}/components`, componentIds);
  return res.data;
}

/** 解绑Catalog的组件 */
export async function unbindComponent(catalogId: string, componentId: string): Promise<boolean> {
  const res = await del<boolean>(`${BASE}/${catalogId}/components/${componentId}`);
  return res.data;
}

/** 获取Catalog已绑定的组件ID列表 */
export async function getBoundComponentIds(catalogId: string): Promise<string[]> {
  const res = await get<string[]>(`${BASE}/${catalogId}/component-ids`);
  return res.data ?? [];
}

/** 获取Catalog已绑定的组件详情列表 */
export async function getBoundComponents(catalogId: string): Promise<CatalogComponentInfo[]> {
  const res = await get<CatalogComponentInfo[]>(`${BASE}/${catalogId}/components`);
  return res.data ?? [];
}

// =================== Component 接口 ===================

/** 查询组件列表 */
export async function getComponentList(): Promise<CatalogComponentInfo[]> {
  const res = await get<CatalogComponentInfo[]>(`${COMP_BASE}/list`);
  return res.data ?? [];
}

/** 根据ID查询组件 */
export async function getComponentById(id: string): Promise<CatalogComponentInfo> {
  const res = await get<CatalogComponentInfo>(`${COMP_BASE}/${id}`);
  return res.data;
}

/** 新增或更新组件 */
export async function saveOrUpdateComponent(data: CatalogComponentInfo): Promise<string> {
  const res = await post<string>(COMP_BASE, data);
  return res.data;
}

/** 批量删除组件 */
export async function deleteComponents(ids: string[]): Promise<boolean> {
  const res = await del<boolean>(COMP_BASE, ids);
  return res.data;
}
