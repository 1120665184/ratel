import { getFileInfo, post } from '@gwsu/core';
import type {
  KnowledgeChunkAdjacentDTO,
  KnowledgeDocumentQuery,
  KnowledgeDocumentRoleSaveDTO,
  KnowledgeDocumentSaveDTO,
  KnowledgeDocumentVO,
  KnowledgePageDetailVO,
  KnowledgePageQuery,
  KnowledgePageSaveDTO,
  KnowledgePageVO,
  KnowledgeSearchDTO,
  KnowledgeSearchResultVO,
  PageResult,
} from '../types';

const BASE = '/kit/knowledge';

export async function saveKnowledgeDocument(data: KnowledgeDocumentSaveDTO): Promise<string> {
  const res = await post<string>(`${BASE}/document/save`, data);
  return res.data;
}

export async function saveKnowledgeDocumentRoles(
  data: KnowledgeDocumentRoleSaveDTO,
): Promise<void> {
  await post<void>(`${BASE}/document/role/save`, data);
}

export async function getKnowledgeDocumentPage(
  query: KnowledgeDocumentQuery,
): Promise<PageResult<KnowledgeDocumentVO>> {
  const res = await post<PageResult<KnowledgeDocumentVO>>(`${BASE}/document/page`, query);
  return res.data;
}

export async function getKnowledgeDocument(documentId: string): Promise<KnowledgeDocumentVO> {
  const res = await post<KnowledgeDocumentVO>(`${BASE}/document/${documentId}`, {});
  return res.data;
}

export async function enableKnowledgeDocument(documentId: string): Promise<void> {
  await post<void>(`${BASE}/document/enable/${documentId}`, {});
}

export async function disableKnowledgeDocument(documentId: string): Promise<void> {
  await post<void>(`${BASE}/document/disable/${documentId}`, {});
}

export async function deleteKnowledgeDocument(documentId: string): Promise<void> {
  await post<void>(`${BASE}/document/delete/${documentId}`, {});
}

export async function retryKnowledgeTask(taskId: string): Promise<string> {
  const res = await post<string>(`${BASE}/task/retry/${taskId}`, {});
  return res.data;
}

export async function getKnowledgePagePage(
  query: KnowledgePageQuery,
): Promise<PageResult<KnowledgePageVO>> {
  const res = await post<PageResult<KnowledgePageVO>>(`${BASE}/page/page`, query);
  return res.data;
}

export async function getKnowledgePage(pageId: string): Promise<KnowledgePageDetailVO> {
  const res = await post<KnowledgePageDetailVO>(`${BASE}/page/${pageId}`, {});
  return res.data;
}

export async function saveKnowledgePage(data: KnowledgePageSaveDTO): Promise<string> {
  const res = await post<string>(`${BASE}/page/save`, data);
  return res.data;
}

export async function searchKnowledge(
  query: KnowledgeSearchDTO,
): Promise<KnowledgeSearchResultVO[]> {
  const res = await post<KnowledgeSearchResultVO[]>(`${BASE}/search`, query);
  return res.data ?? [];
}

export async function findAdjacentKnowledgeChunk(
  data: KnowledgeChunkAdjacentDTO,
): Promise<KnowledgeSearchResultVO | null> {
  const res = await post<KnowledgeSearchResultVO | null>(`${BASE}/chunk/adjacent`, data);
  return res.data ?? null;
}

export async function resolveFileName(fileId: string): Promise<string> {
  const res = await getFileInfo(fileId);
  return res.data?.fileName ?? '';
}
