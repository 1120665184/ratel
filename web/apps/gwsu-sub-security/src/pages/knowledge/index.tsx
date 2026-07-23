import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Descriptions,
  Dropdown,
  Drawer,
  Empty,
  Form,
  Input,
  List,
  Modal,
  Select,
  Segmented,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import type { MenuProps, TableProps } from 'antd';
import {
  CheckCircleOutlined,
  CloudUploadOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  MoreOutlined,
  ReloadOutlined,
  RetweetOutlined,
  SaveOutlined,
  SearchOutlined,
  StopOutlined,
} from '@ant-design/icons';
import { AuthGate, FileScope, FileUpload, useAuth } from '@gwsu/core';
import MarkdownPreview from '../../components/MarkdownPreview';
import styles from './index.module.less';
import {
  findAdjacentKnowledgeChunk,
  deleteKnowledgeDocument,
  disableKnowledgeDocument,
  enableKnowledgeDocument,
  getKnowledgeDocumentPage,
  getKnowledgePage,
  getKnowledgePagePage,
  resolveFileName,
  retryKnowledgeTask,
  saveKnowledgeDocument,
  saveKnowledgeDocumentRoles,
  saveKnowledgePage,
  searchKnowledge,
} from './services/knowledge';
import { getRoleList } from '../role/services/role';
import {
  BLOCK_TYPE_OPTIONS,
  DOCUMENT_STATUS_OPTIONS,
  INGEST_STAGE_LABEL_MAP,
  PAGE_STATUS_OPTIONS,
  TASK_STATUS_OPTIONS,
  type KnowledgeDocumentQuery,
  type KnowledgeDocumentVO,
  type KnowledgePageBlockVO,
  type KnowledgePageDetailVO,
  type KnowledgePageQuery,
  type KnowledgePageVO,
  type KnowledgeSearchResultVO,
  type PageResult,
} from './types';
import type { RoleInfo } from '../role/types';
import {
  PERM_KNOWLEDGE_PAGE_EDIT,
  PERM_KNOWLEDGE_ROLE_EDIT,
  PERM_KNOWLEDGE_TASK_RETRY,
  PERM_KNOWLEDGE_UPLOAD,
} from './permissionConstants';

const { Paragraph, Text, Title } = Typography;
const MORE_ACTION_RETRY = 'retry';
const MORE_ACTION_ROLE = 'role';
const MORE_ACTION_TOGGLE_ENABLED = 'toggleEnabled';
const MORE_ACTION_DELETE = 'delete';

const defaultPage = <T,>(): PageResult<T> => ({
  records: [],
  total: 0,
  size: 10,
  current: 1,
  pages: 0,
});

function splitCodes(value?: string[]): string[] | undefined {
  const codes = (value ?? []).map((item) => item.trim()).filter(Boolean);
  return codes.length > 0 ? codes : undefined;
}

function getStatusMeta<T extends string>(
  options: Array<{ label: string; value: T; color: string }>,
  value?: T,
) {
  return options.find((item) => item.value === value);
}

function buildPageContent(blocks: KnowledgePageBlockVO[], fallback?: string): string {
  if (blocks.length === 0) {
    return fallback ?? '';
  }
  return blocks.map((block) => block.content).join('\n\n');
}

function normalizeHeadingText(value?: string): string {
  return (value ?? '')
    .replace(/^#+\s*/, '')
    .trim()
    .toLowerCase();
}

function stripDuplicatePageTitle(content: string, pageTitle?: string): string {
  const normalizedTitle = normalizeHeadingText(pageTitle);
  if (!normalizedTitle) {
    return content;
  }

  const lines = content.split('\n');
  const firstNonEmptyIndex = lines.findIndex((line) => line.trim());
  if (firstNonEmptyIndex < 0) {
    return content;
  }

  const firstLine = lines[firstNonEmptyIndex].trim();
  const headingMatch = firstLine.match(/^#\s+(.+)$/);
  if (!headingMatch) {
    return content;
  }

  if (normalizeHeadingText(headingMatch[1]) !== normalizedTitle) {
    return content;
  }

  const remainingLines = lines.slice(firstNonEmptyIndex + 1);
  while (remainingLines.length > 0 && !remainingLines[0].trim()) {
    remainingLines.shift();
  }
  return remainingLines.join('\n');
}

function normalizeKnowledgeImageMarkdown(content: string): string {
  return content.replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, (_, altText: string, src: string) => {
    const matched = src.match(/(?:^|\/)kit\/file\/stream\/([^/?#)]+)/);
    if (!matched?.[1]) {
      return `![${altText}](${src})`;
    }
    return `![${altText}](knowledge_image:fileId=${matched[1]})`;
  });
}

function stripSearchHighlight(content?: string): string {
  return (content ?? '').replace(/<\/?mark>/g, '');
}

const KnowledgePage: React.FC = () => {
  const [documentForm] = Form.useForm<KnowledgeDocumentQuery>();
  const [pageForm] = Form.useForm<KnowledgePageQuery>();
  const [uploadForm] = Form.useForm();
  const [roleForm] = Form.useForm();
  const [searchForm] = Form.useForm();

  const canEditPage = useAuth(PERM_KNOWLEDGE_PAGE_EDIT);
  const canEditRole = useAuth(PERM_KNOWLEDGE_ROLE_EDIT);
  const canRetryTask = useAuth(PERM_KNOWLEDGE_TASK_RETRY);
  const canManageDocument = useAuth(PERM_KNOWLEDGE_UPLOAD);

  const [documentLoading, setDocumentLoading] = useState(false);
  const [documentPage, setDocumentPage] = useState<PageResult<KnowledgeDocumentVO>>(defaultPage);
  const [documentQuery, setDocumentQuery] = useState<KnowledgeDocumentQuery>({
    pageNum: 1,
    pageSize: 10,
  });

  const [pageLoading, setPageLoading] = useState(false);
  const [wikiPage, setWikiPage] = useState<PageResult<KnowledgePageVO>>(defaultPage);
  const [wikiQuery, setWikiQuery] = useState<KnowledgePageQuery>({
    pageNum: 1,
    pageSize: 10,
  });

  const [uploadVisible, setUploadVisible] = useState(false);
  const [uploadSubmitting, setUploadSubmitting] = useState(false);
  const [roleVisible, setRoleVisible] = useState(false);
  const [roleSubmitting, setRoleSubmitting] = useState(false);
  const [currentDocument, setCurrentDocument] = useState<KnowledgeDocumentVO | null>(null);
  const [roleOptions, setRoleOptions] = useState<Array<{ label: string; value: string }>>([]);

  const [detailVisible, setDetailVisible] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailSubmitting, setDetailSubmitting] = useState(false);
  const [pageDetail, setPageDetail] = useState<KnowledgePageDetailVO | null>(null);
  const [editingBlocks, setEditingBlocks] = useState<KnowledgePageBlockVO[]>([]);
  const [contentViewMode, setContentViewMode] = useState<'preview' | 'markdown'>('preview');

  const [searchLoading, setSearchLoading] = useState(false);
  const [searchResults, setSearchResults] = useState<KnowledgeSearchResultVO[]>([]);
  const [searchDetailMode, setSearchDetailMode] = useState<'preview' | 'markdown'>('preview');
  const [selectedSearchResult, setSelectedSearchResult] = useState<KnowledgeSearchResultVO | null>(null);
  const [searchDetailBlocks, setSearchDetailBlocks] = useState<KnowledgeSearchResultVO[]>([]);
  const [searchAdjacentLoading, setSearchAdjacentLoading] = useState<'PREVIOUS' | 'NEXT' | ''>('');
  const [searchBoundaryReached, setSearchBoundaryReached] = useState({
    PREVIOUS: false,
    NEXT: false,
  });

  const roleLabelMap = useMemo(
    () =>
      roleOptions.reduce<Record<string, string>>((accumulator, option) => {
        accumulator[option.value] = option.label;
        return accumulator;
      }, {}),
    [roleOptions],
  );

  const renderRoleTags = useCallback(
    (roleCodes?: string[]) =>
      roleCodes?.length
        ? roleCodes.map((code) => <Tag key={code}>{roleLabelMap[code] ?? code}</Tag>)
        : <Tag color="green">开放</Tag>,
    [roleLabelMap],
  );

  const loadDocuments = useCallback(async (query: KnowledgeDocumentQuery) => {
    setDocumentLoading(true);
    try {
      const data = await getKnowledgeDocumentPage(query);
      setDocumentPage(data ?? defaultPage());
    } finally {
      setDocumentLoading(false);
    }
  }, []);

  const loadPages = useCallback(async (query: KnowledgePageQuery) => {
    setPageLoading(true);
    try {
      const data = await getKnowledgePagePage(query);
      setWikiPage(data ?? defaultPage());
    } finally {
      setPageLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadDocuments(documentQuery);
  }, [documentQuery, loadDocuments]);

  useEffect(() => {
    void loadPages(wikiQuery);
  }, [loadPages, wikiQuery]);

  useEffect(() => {
    const loadRoles = async () => {
      try {
        const roles = await getRoleList(1);
        setRoleOptions(
          (roles ?? []).map((item: RoleInfo) => ({
            label: `${item.roleName} (${item.roleCode})`,
            value: item.roleCode,
          })),
        );
      } catch {
        message.warning('角色列表加载失败，角色选择可能不可用');
      }
    };
    void loadRoles();
  }, []);

  const refreshAll = useCallback(() => {
    void loadDocuments(documentQuery);
    void loadPages(wikiQuery);
  }, [documentQuery, loadDocuments, loadPages, wikiQuery]);

  const handleDocumentSearch = useCallback(() => {
    const values = documentForm.getFieldsValue();
    const nextQuery = { ...documentQuery, ...values, pageNum: 1 };
    setDocumentQuery(nextQuery);
  }, [documentForm, documentQuery]);

  const handleDocumentReset = useCallback(() => {
    documentForm.resetFields();
    const nextQuery = { pageNum: 1, pageSize: documentQuery.pageSize };
    setDocumentQuery(nextQuery);
  }, [documentForm, documentQuery.pageSize]);

  const handlePageSearch = useCallback(() => {
    const values = pageForm.getFieldsValue();
    const nextQuery = { ...wikiQuery, ...values, pageNum: 1 };
    setWikiQuery(nextQuery);
  }, [pageForm, wikiQuery]);

  const handlePageReset = useCallback(() => {
    pageForm.resetFields();
    const nextQuery = { pageNum: 1, pageSize: wikiQuery.pageSize };
    setWikiQuery(nextQuery);
  }, [pageForm, wikiQuery.pageSize]);

  const handleOpenUpload = useCallback(() => {
    uploadForm.resetFields();
    setUploadVisible(true);
  }, [uploadForm]);

  const handleUploadedFileChange = useCallback(async (fileIds: string[]) => {
    const fileId = fileIds[0];
    uploadForm.setFieldValue('fileId', fileId);
    if (!fileId) {
      return;
    }
    try {
      const fileName = await resolveFileName(fileId);
      if (fileName) {
        uploadForm.setFieldValue('fileName', fileName);
      }
    } catch {
      message.warning('文件名自动读取失败，请手动填写文件名');
    }
  }, [uploadForm]);

  const handleSubmitUpload = useCallback(async () => {
    const values = await uploadForm.validateFields();
    setUploadSubmitting(true);
    try {
      await saveKnowledgeDocument({
        fileId: values.fileId,
        fileName: values.fileName,
        roleCodes: splitCodes(values.roleCodes),
      });
      message.success('文档已提交导入');
      setUploadVisible(false);
      uploadForm.resetFields();
      refreshAll();
    } finally {
      setUploadSubmitting(false);
    }
  }, [refreshAll, uploadForm]);

  const handleOpenRole = useCallback((record: KnowledgeDocumentVO) => {
    setCurrentDocument(record);
    roleForm.setFieldsValue({
      roleCodes: record.roleCodes ?? [],
    });
    setRoleVisible(true);
  }, [roleForm]);

  const handleSubmitRole = useCallback(async () => {
    if (!currentDocument) {
      return;
    }
    const values = await roleForm.validateFields();
    setRoleSubmitting(true);
    try {
      await saveKnowledgeDocumentRoles({
        sourceDocumentId: currentDocument.id,
        roleCodes: splitCodes(values.roleCodes),
      });
      message.success('文档角色权限已更新');
      setRoleVisible(false);
      setCurrentDocument(null);
      void loadDocuments(documentQuery);
    } finally {
      setRoleSubmitting(false);
    }
  }, [currentDocument, documentQuery, loadDocuments, roleForm]);

  const handleRetryDocument = useCallback(async (record: KnowledgeDocumentVO) => {
    if (!record.latestTaskId) {
      message.warning('当前文档没有可重试的导入任务');
      return;
    }
    await retryKnowledgeTask(record.latestTaskId);
    message.success('已重新提交导入任务');
    refreshAll();
  }, [refreshAll]);

  const handleToggleDocumentEnabled = useCallback(async (record: KnowledgeDocumentVO) => {
    if (record.enabled === false) {
      await enableKnowledgeDocument(record.id);
      message.success('文档已启用');
    } else {
      await disableKnowledgeDocument(record.id);
      message.success('文档已禁用，相关 Chunk 已从检索索引移除');
    }
    refreshAll();
  }, [refreshAll]);

  const handleDeleteDocument = useCallback(async (record: KnowledgeDocumentVO) => {
    await deleteKnowledgeDocument(record.id);
    message.success('文档已删除，相关 Chunk 已从检索索引移除');
    refreshAll();
  }, [refreshAll]);

  const handleMoreAction = useCallback(async (key: string, record: KnowledgeDocumentVO) => {
    if (key === MORE_ACTION_RETRY) {
      await handleRetryDocument(record);
      return;
    }
    if (key === MORE_ACTION_ROLE) {
      handleOpenRole(record);
      return;
    }
    if (key === MORE_ACTION_TOGGLE_ENABLED) {
      await handleToggleDocumentEnabled(record);
      return;
    }
    if (key === MORE_ACTION_DELETE) {
      await handleDeleteDocument(record);
    }
  }, [handleDeleteDocument, handleOpenRole, handleRetryDocument, handleToggleDocumentEnabled]);

  const handleOpenPageDetail = useCallback(async (record: KnowledgePageVO) => {
    setDetailVisible(true);
    setDetailLoading(true);
    setContentViewMode('preview');
    try {
      const detail = await getKnowledgePage(record.id);
      setPageDetail(detail);
      setEditingBlocks((detail.blocks ?? []).map((block) => ({ ...block })));
    } finally {
      setDetailLoading(false);
    }
  }, []);

  const handleChangeBlock = useCallback(
    (index: number, patch: Partial<KnowledgePageBlockVO>) => {
      setEditingBlocks((prev) =>
        prev.map((block, currentIndex) =>
          currentIndex === index ? { ...block, ...patch } : block,
        ),
      );
    },
    [],
  );

  const handleSavePage = useCallback(async () => {
    if (!pageDetail) {
      return;
    }
    setDetailSubmitting(true);
    try {
      await saveKnowledgePage({
        id: pageDetail.id,
        title: pageDetail.title,
        blocks: editingBlocks.map((block) => ({
          ...block,
          content: normalizeKnowledgeImageMarkdown(block.content),
        })),
      });
      message.success('Wiki Page 已保存并重建 Chunk 索引');
      const detail = await getKnowledgePage(pageDetail.id);
      setPageDetail(detail);
      setEditingBlocks((detail.blocks ?? []).map((block) => ({ ...block })));
      void loadPages(wikiQuery);
    } finally {
      setDetailSubmitting(false);
    }
  }, [editingBlocks, loadPages, pageDetail, wikiQuery]);

  const handleSearchKnowledge = useCallback(async () => {
    const values = await searchForm.validateFields();
    setSearchLoading(true);
    setSelectedSearchResult(null);
    setSearchDetailBlocks([]);
    setSearchBoundaryReached({ PREVIOUS: false, NEXT: false });
    try {
      const data = await searchKnowledge({
        pageNum: 1,
        pageSize: values.size ?? 10,
        keyword: values.keyword,
        roleCodes: splitCodes(values.roleCodes),
        size: values.size ?? 10,
      });
      setSearchResults(data);
      if (data.length === 0) {
        message.info('没有检索到匹配的 Chunk');
      }
    } finally {
      setSearchLoading(false);
    }
  }, [searchForm]);

  const handleOpenSearchDetail = useCallback((record: KnowledgeSearchResultVO) => {
    setSelectedSearchResult(record);
    setSearchDetailBlocks([{ ...record, content: stripSearchHighlight(record.content) }]);
    setSearchDetailMode('preview');
    setSearchBoundaryReached({ PREVIOUS: false, NEXT: false });
  }, []);

  const handleBackToSearchList = useCallback(() => {
    setSelectedSearchResult(null);
    setSearchDetailBlocks([]);
    setSearchBoundaryReached({ PREVIOUS: false, NEXT: false });
  }, []);

  const handleLoadAdjacentBlocks = useCallback(
    async (direction: 'PREVIOUS' | 'NEXT') => {
      if (searchDetailBlocks.length === 0) {
        return;
      }
      const boundaryBlock =
        direction === 'PREVIOUS'
          ? searchDetailBlocks[0]
          : searchDetailBlocks[searchDetailBlocks.length - 1];
      if (!boundaryBlock.pageBlockId) {
        message.warning('当前内容缺少 Block 标识，无法继续加载相邻内容');
        return;
      }
      const values = searchForm.getFieldsValue();
      setSearchAdjacentLoading(direction);
      try {
        const data = await findAdjacentKnowledgeChunk({
          pageBlockId: boundaryBlock.pageBlockId,
          direction,
          offset: 1,
          roleCodes: splitCodes(values.roleCodes),
        });
        const normalizedBlocks = data.map((item) => ({
          ...item,
          content: stripSearchHighlight(item.content),
        }));
        if (normalizedBlocks.length === 0) {
          setSearchBoundaryReached((prev) => ({ ...prev, [direction]: true }));
          message.info(direction === 'PREVIOUS' ? '已经是第一段内容' : '已经是最后一段内容');
          return;
        }
        setSearchBoundaryReached((prev) => ({ ...prev, [direction]: false }));
        setSearchDetailBlocks((prev) => {
          const existingIds = new Set(prev.map((item) => item.pageBlockId).filter(Boolean));
          const deduplicated = normalizedBlocks.filter(
            (item) => !item.pageBlockId || !existingIds.has(item.pageBlockId),
          );
          if (deduplicated.length === 0) {
            return prev;
          }
          return direction === 'PREVIOUS'
            ? [...deduplicated.reverse(), ...prev]
            : [...prev, ...deduplicated];
        });
      } finally {
        setSearchAdjacentLoading('');
      }
    },
    [searchDetailBlocks, searchForm],
  );

  const pageMarkdownContent = useMemo(
    () => buildPageContent(editingBlocks, pageDetail?.markdownContent),
    [editingBlocks, pageDetail?.markdownContent],
  );

  const pagePreviewContent = useMemo(
    () => stripDuplicatePageTitle(pageMarkdownContent, pageDetail?.title),
    [pageDetail?.title, pageMarkdownContent],
  );

  const searchDetailMarkdownContent = useMemo(
    () => searchDetailBlocks.map((item) => item.content).join('\n\n'),
    [searchDetailBlocks],
  );

  const searchDetailPreviewContent = useMemo(
    () => stripDuplicatePageTitle(searchDetailMarkdownContent, selectedSearchResult?.title),
    [searchDetailMarkdownContent, selectedSearchResult?.title],
  );

  const documentColumns: TableProps<KnowledgeDocumentVO>['columns'] = useMemo(() => [
    {
      title: '文件名',
      dataIndex: 'fileName',
      ellipsis: true,
      width: 260,
    },
    {
      title: '状态',
      dataIndex: 'documentStatus',
      width: 100,
      render: (value) => {
        const meta = getStatusMeta(DOCUMENT_STATUS_OPTIONS, value);
        return <Tag color={meta?.color}>{meta?.label ?? value}</Tag>;
      },
    },
    {
      title: '向量化',
      dataIndex: 'embeddingCompleted',
      width: 90,
      render: (value) => value ? <Tag color="success">已完成</Tag> : <Tag color="warning">未完成</Tag>,
    },
    {
      title: '图片 OCR',
      dataIndex: 'imageOcrParsed',
      width: 100,
      render: (value) => value ? <Tag color="success">已解析</Tag> : <Tag color="default">未解析</Tag>,
    },
    {
      title: '最近导入',
      dataIndex: 'latestTaskStatus',
      width: 170,
      render: (value, record) => {
        if (!value) {
          return '-';
        }
        const meta = getStatusMeta(TASK_STATUS_OPTIONS, value);
        return (
          <Space size={4} wrap>
            <Tag color={meta?.color}>{meta?.label ?? value}</Tag>
            {record.latestTaskStage && <Tag>{INGEST_STAGE_LABEL_MAP[record.latestTaskStage] ?? record.latestTaskStage}</Tag>}
          </Space>
        );
      },
    },
    {
      title: '授权角色',
      dataIndex: 'roleCodes',
      width: 220,
      render: (value: string[] | undefined) => renderRoleTags(value),
    },
    {
      title: '处理信息',
      dataIndex: 'processMessage',
      ellipsis: true,
      width: 240,
      render: (value, record) => value || record.latestTaskErrorMessage || '-',
    },
    {
      title: '开始时间',
      dataIndex: 'latestTaskStartedAt',
      width: 180,
      render: (value) => value || '-',
    },
    {
      title: '完成时间',
      dataIndex: 'latestTaskFinishedAt',
      width: 180,
      render: (value, record) => value || record.processedAt || '-',
    },
    {
      title: '启用',
      dataIndex: 'enabled',
      width: 80,
      fixed: 'right',
      render: (value) => value === false ? <Tag color="default">禁用</Tag> : <Tag color="success">启用</Tag>,
    },
    {
      title: '操作',
      width: 260,
      fixed: 'right',
      render: (_, record) => {
        const menuItems: MenuProps['items'] = [];
        if (canRetryTask && record.latestTaskId) {
          menuItems.push({
            key: MORE_ACTION_RETRY,
            label: '重新导入',
            icon: <RetweetOutlined />,
          });
        }
        if (canEditRole) {
          menuItems.push({
            key: MORE_ACTION_ROLE,
            label: '权限',
            icon: <EditOutlined />,
          });
        }
        if (canManageDocument) {
          menuItems.push({
            key: MORE_ACTION_TOGGLE_ENABLED,
            label: record.enabled === false ? '启用' : '禁用',
            icon: record.enabled === false ? <CheckCircleOutlined /> : <StopOutlined />,
          });
          menuItems.push({
            key: MORE_ACTION_DELETE,
            label: '删除',
            icon: <DeleteOutlined />,
            danger: true,
          });
        }

        return (
          <Space size="small">
            <Button type="link" size="small" icon={<EyeOutlined />} disabled>
              查看
            </Button>
            <Dropdown
              menu={{
                items: menuItems,
                onClick: async ({ key }) => {
                  if (key === MORE_ACTION_RETRY) {
                    Modal.confirm({
                      title: '确认重新导入这个文档？',
                      okText: '重新导入',
                      cancelText: '取消',
                      okButtonProps: { 'data-ai-approval': 'true' },
                      onOk: () => handleMoreAction(key, record),
                    });
                    return;
                  }
                  if (key === MORE_ACTION_TOGGLE_ENABLED) {
                    Modal.confirm({
                      title: record.enabled === false ? '确认启用这个文档？' : '确认禁用这个文档？',
                      okText: record.enabled === false ? '启用' : '禁用',
                      cancelText: '取消',
                      okButtonProps: { 'data-ai-approval': 'true' },
                      onOk: () => handleMoreAction(key, record),
                    });
                    return;
                  }
                  if (key === MORE_ACTION_DELETE) {
                    Modal.confirm({
                      title: '确认删除这个文档？',
                      content: '删除后源文档和任务会软删除，相关 Chunk 会从 ES 检索索引移除；历史 Page 版本保留用于审计。',
                      okText: '删除',
                      cancelText: '取消',
                      okType: 'danger',
                      okButtonProps: { 'data-ai-approval': 'true' },
                      onOk: () => handleMoreAction(key, record),
                    });
                    return;
                  }
                  await handleMoreAction(key, record);
                },
              }}
              disabled={menuItems.length === 0}
              trigger={['click']}
            >
              <Button type="link" size="small" icon={<MoreOutlined />}>
                更多
              </Button>
            </Dropdown>
          </Space>
        );
      },
    },
  ], [canEditRole, canManageDocument, canRetryTask, handleMoreAction, renderRoleTags]);

  const pageColumns: TableProps<KnowledgePageVO>['columns'] = useMemo(() => [
    {
      title: '标题',
      dataIndex: 'title',
      ellipsis: true,
      width: 280,
    },
    {
      title: '源文档',
      dataIndex: 'sourceDocumentName',
      ellipsis: true,
      width: 240,
      render: (value) => value || '-',
    },
    {
      title: '状态',
      dataIndex: 'pageStatus',
      width: 100,
      render: (value) => {
        const meta = getStatusMeta(PAGE_STATUS_OPTIONS, value);
        return <Tag color={meta?.color}>{meta?.label ?? value}</Tag>;
      },
    },
    {
      title: '当前版本',
      dataIndex: 'currentVersionId',
      width: 220,
      ellipsis: true,
      render: (value) => value || '-',
    },
    {
      title: '更新时间',
      dataIndex: 'modifyTime',
      width: 180,
      render: (value) => value || '-',
    },
    {
      title: '操作',
      width: 120,
      fixed: 'right',
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => handleOpenPageDetail(record)}
        >
          查看
        </Button>
      ),
    },
  ], [handleOpenPageDetail]);

  const documentTab = (
    <div className={styles.panel}>
      <div className={styles.searchBar}>
        <Form form={documentForm} layout="inline" component={false}>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>文件名</span>
            <Form.Item name="fileName" noStyle>
              <Input className={styles.searchInput} allowClear placeholder="请输入文件名" onPressEnter={handleDocumentSearch} />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>状态</span>
            <Form.Item name="documentStatus" noStyle>
              <Select
                className={styles.searchSelect}
                allowClear
                placeholder="全部"
                options={DOCUMENT_STATUS_OPTIONS.map(({ label, value }) => ({ label, value }))}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>启用状态</span>
            <Form.Item name="enabled" noStyle>
              <Select
                className={styles.searchSelect}
                allowClear
                placeholder="全部"
                options={[
                  { label: '启用', value: true },
                  { label: '禁用', value: false },
                ]}
              />
            </Form.Item>
          </div>
        </Form>
        <div className={styles.searchActions}>
          <Button type="primary" icon={<SearchOutlined />} onClick={handleDocumentSearch}>查询</Button>
          <Button icon={<ReloadOutlined />} onClick={handleDocumentReset}>重置</Button>
          <AuthGate buttonKey={PERM_KNOWLEDGE_UPLOAD}>
            <Button type="primary" icon={<CloudUploadOutlined />} onClick={handleOpenUpload}>上传文档</Button>
          </AuthGate>
        </div>
      </div>
      <div className={styles.tableWrapper}>
        <Table<KnowledgeDocumentVO>
          rowKey="id"
          columns={documentColumns}
          dataSource={documentPage.records}
          loading={documentLoading}
          scroll={{ x: 1600 }}
          pagination={{
            current: documentPage.current,
            pageSize: documentPage.size,
            total: documentPage.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (pageNum, pageSize) => {
              const nextQuery = { ...documentQuery, pageNum, pageSize };
              setDocumentQuery(nextQuery);
            },
          }}
        />
      </div>
    </div>
  );

  const pageTab = (
    <div className={styles.panel}>
      <div className={styles.searchBar}>
        <Form form={pageForm} layout="inline" component={false}>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>标题</span>
            <Form.Item name="title" noStyle>
              <Input className={styles.searchInput} allowClear placeholder="请输入标题" onPressEnter={handlePageSearch} />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>状态</span>
            <Form.Item name="pageStatus" noStyle>
              <Select
                className={styles.searchSelect}
                allowClear
                placeholder="全部"
                options={PAGE_STATUS_OPTIONS.map(({ label, value }) => ({ label, value }))}
              />
            </Form.Item>
          </div>
        </Form>
        <div className={styles.searchActions}>
          <Button type="primary" icon={<SearchOutlined />} onClick={handlePageSearch}>查询</Button>
          <Button icon={<ReloadOutlined />} onClick={handlePageReset}>重置</Button>
          <Button icon={<ReloadOutlined />} onClick={() => void loadPages(wikiQuery)}>刷新</Button>
        </div>
      </div>
      <div className={styles.tableWrapper}>
        <Table<KnowledgePageVO>
          rowKey="id"
          columns={pageColumns}
          dataSource={wikiPage.records}
          loading={pageLoading}
          scroll={{ x: 900 }}
          pagination={{
            current: wikiPage.current,
            pageSize: wikiPage.size,
            total: wikiPage.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (pageNum, pageSize) => {
              const nextQuery = { ...wikiQuery, pageNum, pageSize };
              setWikiQuery(nextQuery);
            },
          }}
        />
      </div>
    </div>
  );

  const searchTab = (
    <div className={styles.searchPanel}>
      <div className={styles.searchCard}>
        <Title level={5}>知识库检索调试</Title>
        <Form form={searchForm} layout="vertical" initialValues={{ size: 10 }}>
          <Form.Item name="keyword" label="关键词" rules={[{ required: true, message: '请输入关键词' }]}>
            <Input.Search
              allowClear
              placeholder="请输入要检索的问题或关键词"
              enterButton="检索"
              loading={searchLoading}
              onSearch={handleSearchKnowledge}
            />
          </Form.Item>
          <Form.Item name="roleCodes" label="当前检索角色">
            <Select
              mode="multiple"
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="为空表示开放角色；选择后仅检索有权限的源文档"
              options={roleOptions}
            />
          </Form.Item>
          <Form.Item name="size" label="返回数量">
            <Select
              options={[5, 10, 20, 50].map((value) => ({ label: `${value} 条`, value }))}
            />
          </Form.Item>
        </Form>
        <Alert
          type="info"
          showIcon
          message="检索权限说明"
          description="前端只传 roleCodes，后端会基于源文档权限过滤可见 Chunk；Chunk 自身不保存 role_codes。"
        />
      </div>
      <div className={styles.resultCard}>
        <div className={styles.resultHeader}>
          <span className={styles.tableTitle}>检索结果</span>
          <Text type="secondary">{searchResults.length} 条</Text>
        </div>
        {searchResults.length === 0 ? (
          <Empty description="暂无结果" />
        ) : selectedSearchResult ? (
          <div className={styles.searchDetailCard}>
            <div className={styles.searchDetailHeader}>
              <div>
                <Text strong>{selectedSearchResult.title}</Text>
                {selectedSearchResult.headingPath && (
                  <div className={styles.searchDetailSubTitle}>
                    <Text type="secondary">{selectedSearchResult.headingPath}</Text>
                  </div>
                )}
              </div>
              <Space wrap>
                <Button onClick={handleBackToSearchList}>返回列表</Button>
                <Button
                  loading={searchAdjacentLoading === 'PREVIOUS'}
                  disabled={searchBoundaryReached.PREVIOUS}
                  onClick={() => void handleLoadAdjacentBlocks('PREVIOUS')}
                >
                  加载上一段
                </Button>
                <Button
                  loading={searchAdjacentLoading === 'NEXT'}
                  disabled={searchBoundaryReached.NEXT}
                  onClick={() => void handleLoadAdjacentBlocks('NEXT')}
                >
                  加载下一段
                </Button>
              </Space>
            </div>
            <Descriptions column={2} size="small" bordered className={styles.searchDetailMeta}>
              <Descriptions.Item label="Page ID">{selectedSearchResult.pageId || '-'}</Descriptions.Item>
              <Descriptions.Item label="命中 Block">{selectedSearchResult.pageBlockId || '-'}</Descriptions.Item>
              <Descriptions.Item label="源文档ID">{selectedSearchResult.sourceDocumentId || '-'}</Descriptions.Item>
              <Descriptions.Item label="Block 序号">{selectedSearchResult.blockOrder ?? '-'}</Descriptions.Item>
            </Descriptions>
            <div className={styles.contentTab}>
              <div className={styles.contentToolbar}>
                <Segmented
                  size="small"
                  options={[
                    { label: '预览', value: 'preview' },
                    { label: 'MD 原文', value: 'markdown' },
                  ]}
                  value={searchDetailMode}
                  onChange={(value) => setSearchDetailMode(value as 'preview' | 'markdown')}
                />
              </div>
              {searchDetailMode === 'preview' ? (
                <MarkdownPreview
                  className={styles.markdownArticle}
                  content={searchDetailPreviewContent}
                />
              ) : (
                <Input.TextArea
                  className={styles.markdownSource}
                  value={searchDetailMarkdownContent || '暂无内容'}
                  readOnly
                  autoSize={{ minRows: 18, maxRows: 30 }}
                />
              )}
            </div>
          </div>
        ) : (
          <List
            dataSource={searchResults}
            renderItem={(item) => {
              return (
                <List.Item className={styles.resultItem}>
                  <button
                    type="button"
                    className={styles.resultButton}
                    onClick={() => handleOpenSearchDetail(item)}
                  >
                    <div className={styles.resultTitle}>
                      <Text strong>{item.title}</Text>
                      {typeof item.score === 'number' && <Tag color="blue">score {item.score.toFixed(4)}</Tag>}
                      {typeof item.blockOrder === 'number' && <Tag>Block #{item.blockOrder}</Tag>}
                      {typeof item.chunkOrder === 'number' && <Tag>Chunk #{item.chunkOrder}</Tag>}
                    </div>
                    {item.headingPath && <Text type="secondary">{item.headingPath}</Text>}
                    <Paragraph className={styles.chunkText}>{stripSearchHighlight(item.content)}</Paragraph>
                    <div className={styles.metaLine}>
                      <Text type="secondary">pageBlockId：{item.pageBlockId ?? '-'}</Text>
                      <Text type="secondary">sourceDocumentId：{item.sourceDocumentId}</Text>
                    </div>
                  </button>
                </List.Item>
              );
            }}
          />
        )}
      </div>
    </div>
  );

  return (
    <div className={styles.knowledgePage}>
      <div className={styles.pageHeader}>
        <div>
          <Title level={4} className={styles.pageTitle}>
            知识库
          </Title>
          <Text type="secondary">
            上传源文档、维护 Wiki Page，并调试带权限过滤的
            Chunk 检索。
          </Text>
        </div>
        <Button icon={<ReloadOutlined />} onClick={refreshAll}>
          刷新全部
        </Button>
      </div>

      <Tabs
        className={styles.tabs}
        items={[
          { key: "documents", label: "源文档", children: documentTab },
          { key: "pages", label: "Wiki Page", children: pageTab },
          { key: "search", label: "检索调试", children: searchTab },
        ]}
      />

      <Modal
        title="上传知识源文档"
        open={uploadVisible}
        onCancel={() => setUploadVisible(false)}
        onOk={handleSubmitUpload}
        confirmLoading={uploadSubmitting}
        okText="提交导入"
        destroyOnHidden
        okButtonProps={{ "data-ai-approval": "true" }}
      >
        <Form form={uploadForm} layout="vertical">
          <Form.Item label="选择文件">
            <FileUpload
              property={{ scope: FileScope.PROTECTED, categorize: "knowledge" }}
              draggable
              maxCount={1}
              maxSize={500 * 1024 * 1024}
              accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md"
              onChange={handleUploadedFileChange}
            />
          </Form.Item>
          <Form.Item
            name="fileId"
            label="文件ID"
            rules={[{ required: true, message: "请先上传文件" }]}
          >
            <Input disabled placeholder="上传完成后自动填充" />
          </Form.Item>
          <Form.Item
            name="fileName"
            label="文件名"
            rules={[{ required: true, message: "请输入文件名" }]}
          >
            <Input placeholder="上传完成后自动读取，失败时可手动填写" />
          </Form.Item>
          <Form.Item name="roleCodes" label="授权角色编码">
            <Select
              mode="multiple"
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="为空表示开放文档"
              options={roleOptions}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="编辑文档角色权限"
        open={roleVisible}
        onCancel={() => setRoleVisible(false)}
        onOk={handleSubmitRole}
        confirmLoading={roleSubmitting}
        okText="保存"
        destroyOnHidden
        okButtonProps={{ "data-ai-approval": "true" }}
      >
        <Alert
          type="info"
          showIcon
          className={styles.modalAlert}
          message="为空表示开放文档；保存后检索会按源文档权限过滤 Chunk。"
        />
        <Descriptions
          column={1}
          size="small"
          className={styles.modalDescriptions}
        >
          <Descriptions.Item label="源文档ID">
            {currentDocument?.id}
          </Descriptions.Item>
          <Descriptions.Item label="文件名">
            {currentDocument?.fileName}
          </Descriptions.Item>
        </Descriptions>
        <Form form={roleForm} layout="vertical">
          <Form.Item name="roleCodes" label="授权角色编码">
            <Select
              mode="multiple"
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="请选择角色"
              options={roleOptions}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={pageDetail?.title ?? "Wiki Page"}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
        width="72vw"
        destroyOnHidden
      >
        {detailLoading || !pageDetail ? (
          <Empty
            description={detailLoading ? "正在加载 Page 内容" : "暂无内容"}
          />
        ) : (
          <div className={styles.drawerContent}>
            <Descriptions column={2} size="small" bordered>
              <Descriptions.Item label="Page ID">
                {pageDetail.id}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                {pageDetail.pageStatus}
              </Descriptions.Item>
              <Descriptions.Item label="当前版本">
                {pageDetail.currentVersionNo ?? "-"}
              </Descriptions.Item>
              <Descriptions.Item label="发布时间">
                {pageDetail.currentPublishedAt ?? "-"}
              </Descriptions.Item>
            </Descriptions>
            <Tabs
              defaultActiveKey="content"
              items={[
                {
                  key: "content",
                  label: "完整内容",
                  children: (
                    <div className={styles.contentTab}>
                      <div className={styles.contentToolbar}>
                        <Segmented
                          size="small"
                          options={[
                            { label: '预览', value: 'preview' },
                            { label: 'MD 原文', value: 'markdown' },
                          ]}
                          value={contentViewMode}
                          onChange={(value) =>
                            setContentViewMode(value as 'preview' | 'markdown')
                          }
                        />
                      </div>
                      {contentViewMode === 'preview' ? (
                        <MarkdownPreview
                          className={styles.markdownArticle}
                          content={pagePreviewContent}
                        />
                      ) : (
                        <Input.TextArea
                          className={styles.markdownSource}
                          value={pageMarkdownContent || '暂无内容'}
                          readOnly
                          autoSize={{ minRows: 18, maxRows: 30 }}
                        />
                      )}
                    </div>
                  ),
                },
                {
                  key: "blocks",
                  label: canEditPage ? "Block 编辑 / 来源明细" : "来源明细",
                  children: (
                    <>
                      {canEditPage && (
                        <div className={styles.blockToolbar}>
                          <Button
                            type="primary"
                            icon={<SaveOutlined />}
                            loading={detailSubmitting}
                            onClick={() => void handleSavePage()}
                            data-ai-approval
                          >
                            保存 Page
                          </Button>
                        </div>
                      )}
                      <Alert
                        type="warning"
                        showIcon
                        className={styles.blockNotice}
                        message="编辑说明"
                        description="为保证权限边界，前端只允许编辑已有 Block 的内容和类型，不允许新增跨来源 Block 或修改 sourceDocumentId。"
                      />
                      <div className={styles.blockList}>
                        {editingBlocks.map((block, index) => {
                          const structuralHeading = block.blockType === 'HEADING' && !block.sourceDocumentId;
                          return (
                            <div
                              key={block.id ?? index}
                              className={styles.blockCard}
                            >
                            <div className={styles.blockHeader}>
                              <Text strong>Block #{index + 1}</Text>
                              <Space>
                                <Select
                                  className={styles.blockTypeSelect}
                                  value={block.blockType}
                                  disabled={!canEditPage}
                                  options={BLOCK_TYPE_OPTIONS}
                                  onChange={(value) =>
                                    handleChangeBlock(index, {
                                      blockType: value,
                                    })
                                  }
                                />
                                {structuralHeading ? (
                                  <Tag color="purple">结构标题</Tag>
                                ) : (
                                  <Tag>{block.sourceType}</Tag>
                                )}
                              </Space>
                            </div>
                            <div className={styles.blockMeta}>
                              {structuralHeading ? (
                                <Text type="secondary">该标题只用于组织 Wiki 结构，不参与权限过滤和 Chunk 检索。</Text>
                              ) : (
                                <Text type="secondary">
                                  sourceDocumentId：{block.sourceDocumentId}
                                </Text>
                              )}
                              {block.sourceLocator && (
                                <Text type="secondary">
                                  sourceLocator：{block.sourceLocator}
                                </Text>
                              )}
                            </div>
                            <Input.TextArea
                              value={block.content}
                              disabled={!canEditPage}
                              autoSize={{ minRows: 3, maxRows: 12 }}
                              onChange={(event) =>
                                handleChangeBlock(index, {
                                  content: event.target.value,
                                })
                              }
                            />
                          </div>
                          );
                        })}
                      </div>
                    </>
                  ),
                },
              ]}
            />
          </div>
        )}
      </Drawer>
    </div>
  );
};

export default KnowledgePage;
