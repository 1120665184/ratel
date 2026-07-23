export type KnowledgeDocumentStatus = 'UPLOADED' | 'PROCESSING' | 'PROCESSED' | 'FAILED';

export type KnowledgeIngestTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';

export type KnowledgeIngestStage =
  | 'PARSE'
  | 'SANITIZE_SOURCE'
  | 'ANALYZE_SOURCE'
  | 'GENERATE_PAGE'
  | 'MERGE_PAGE'
  | 'BUILD_CHUNK'
  | 'EMBED_CHUNK'
  | 'INDEX_ES';

export type KnowledgePageStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export type KnowledgePageVersionStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export type KnowledgeBlockType = 'HEADING' | 'PARAGRAPH' | 'LIST' | 'TABLE' | 'CODE' | 'QUOTE';

export type KnowledgeSourceType = 'SOURCE_DOCUMENT';

export type KnowledgeChunkDirection = 'PREVIOUS' | 'NEXT';

export interface PageQuery {
  pageNum: number;
  pageSize: number;
  orderByColumn?: string;
  asc?: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface KnowledgeDocumentQuery extends PageQuery {
  fileName?: string;
  documentStatus?: KnowledgeDocumentStatus;
  enabled?: boolean;
}

export interface KnowledgeDocumentSaveDTO {
  id?: string;
  fileId: string;
  fileName: string;
  roleCodes?: string[];
}

export interface KnowledgeDocumentRoleSaveDTO {
  sourceDocumentId: string;
  roleCodes?: string[];
}

export interface KnowledgeDocumentVO {
  id: string;
  fileId: string;
  fileName: string;
  documentStatus: KnowledgeDocumentStatus;
  processMessage?: string;
  embeddingCompleted?: boolean;
  imageOcrParsed?: boolean;
  enabled?: boolean;
  processedAt?: string;
  roleCodes?: string[];
  latestTaskId?: string;
  latestTaskStatus?: KnowledgeIngestTaskStatus;
  latestTaskStage?: KnowledgeIngestStage;
  latestTaskRetryCount?: number;
  latestTaskErrorMessage?: string;
  latestTaskStartedAt?: string;
  latestTaskFinishedAt?: string;
  createTime?: string;
  modifyTime?: string;
}

export interface KnowledgePageQuery extends PageQuery {
  title?: string;
  pageStatus?: KnowledgePageStatus;
}

export interface KnowledgePageBlockVO {
  id?: string;
  pageVersionId?: string;
  orderNo?: number;
  blockType: KnowledgeBlockType;
  content: string;
  sourceType?: KnowledgeSourceType;
  sourceDocumentId?: string;
  sourceLocator?: string;
}

export interface KnowledgePageVO {
  id: string;
  title: string;
  pageStatus: KnowledgePageStatus;
  currentVersionId?: string;
  sourceDocumentName?: string;
  createTime?: string;
  modifyTime?: string;
}

export interface KnowledgePageDetailVO extends KnowledgePageVO {
  markdownContent?: string;
  currentVersionNo?: number;
  currentVersionStatus?: KnowledgePageVersionStatus;
  currentPublishedAt?: string;
  blocks?: KnowledgePageBlockVO[];
}

export interface KnowledgePageSaveDTO {
  id?: string;
  title: string;
  blocks: KnowledgePageBlockVO[];
}

export interface KnowledgeSearchDTO extends PageQuery {
  keyword: string;
  roleCodes?: string[];
  size?: number;
}

export interface KnowledgeSearchResultVO {
  chunkId: string;
  pageId: string;
  pageVersionId?: string;
  pageBlockId?: string;
  blockType?: KnowledgeBlockType;
  sourceDocumentId?: string;
  title?: string;
  headingPath?: string;
  content: string;
  blockOrder?: number;
  chunkOrder?: number;
  score?: number;
}

export interface KnowledgeChunkAdjacentDTO {
  roleCodes?: string[];
  pageBlockId: string;
  direction: KnowledgeChunkDirection;
  offset?: number;
}

export const DOCUMENT_STATUS_OPTIONS: Array<{
  label: string;
  value: KnowledgeDocumentStatus;
  color: string;
}> = [
  { label: '已上传', value: 'UPLOADED', color: 'default' },
  { label: '处理中', value: 'PROCESSING', color: 'processing' },
  { label: '已完成', value: 'PROCESSED', color: 'success' },
  { label: '失败', value: 'FAILED', color: 'error' },
];

export const TASK_STATUS_OPTIONS: Array<{
  label: string;
  value: KnowledgeIngestTaskStatus;
  color: string;
}> = [
  { label: '等待中', value: 'PENDING', color: 'default' },
  { label: '运行中', value: 'RUNNING', color: 'processing' },
  { label: '成功', value: 'SUCCEEDED', color: 'success' },
  { label: '失败', value: 'FAILED', color: 'error' },
];

export const PAGE_STATUS_OPTIONS: Array<{
  label: string;
  value: KnowledgePageStatus;
  color: string;
}> = [
  { label: '草稿', value: 'DRAFT', color: 'default' },
  { label: '已发布', value: 'PUBLISHED', color: 'success' },
  { label: '已归档', value: 'ARCHIVED', color: 'warning' },
];

export const INGEST_STAGE_LABEL_MAP: Record<KnowledgeIngestStage, string> = {
  PARSE: '解析文档',
  SANITIZE_SOURCE: '清洗原文',
  ANALYZE_SOURCE: '分析结构',
  GENERATE_PAGE: '生成 Page',
  MERGE_PAGE: '合并 Page',
  BUILD_CHUNK: '构建 Chunk',
  EMBED_CHUNK: '向量化 Chunk',
  INDEX_ES: '写入 ES',
};

export const BLOCK_TYPE_OPTIONS: Array<{ label: string; value: KnowledgeBlockType }> = [
  { label: '标题', value: 'HEADING' },
  { label: '段落', value: 'PARAGRAPH' },
  { label: '列表', value: 'LIST' },
  { label: '表格', value: 'TABLE' },
  { label: '代码', value: 'CODE' },
  { label: '引用', value: 'QUOTE' },
];
