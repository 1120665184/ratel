import React, { useState, useCallback, useEffect } from 'react';
import { Modal, Table, Input, Select, Tag, Button, Radio, Tooltip, message } from 'antd';
import type { TableProps } from 'antd';
import { SearchOutlined, QuestionCircleOutlined, PlusOutlined } from '@ant-design/icons';
import styles from './index.module.less';
import { getApiResourcePage, getModuleList } from '../../services/apiResource';
import type { ApiResourceItem, ApiResourceQuery, ModuleInfo } from '../../types';

interface ApiResourcePickerProps {
  visible: boolean;
  currentPermission?: string;
  onClose: () => void;
  onConfirm: (permission: string) => void;
}

const METHOD_COLORS: Record<string, string> = {
  GET: 'green',
  POST: 'blue',
  PUT: 'orange',
  DELETE: 'red',
  PATCH: 'purple',
};

/** 生成权限键：METHOD:modulePrefix:path */
const buildPermKey = (item: {
  reqMethod: string;
  modulePrefix: string;
  reqPath: string;
}) => `${item.reqMethod}:${item.modulePrefix}:${item.reqPath}`;

/** 从权限标识字符串解析已选项 */
const parsePermissionString = (permission: string) => {
  return permission
    .split(';')
    .filter(Boolean)
    .map((part) => {
      const isMain = part.startsWith('(main)');
      const purePart = isMain ? part.substring('(main)'.length) : part;
      const [method, modulePrefix, ...pathParts] = purePart.split(':');
      const path = pathParts.join(':');
      const key = `${method}:${modulePrefix}:${path}`;
      return { key, method, modulePrefix, path, isMain };
    });
};

/** 根据解析结果创建最小 ApiResourceItem（用于回显） */
const createItemFromParsed = (parsed: {
  key: string;
  method: string;
  modulePrefix: string;
  path: string;
}): ApiResourceItem => ({
  id: parsed.key,
  reqMethod: parsed.method,
  modulePrefix: parsed.modulePrefix,
  reqPath: parsed.path,
  tagName: '',
  summary: '',
  loginAllowAccess: 0,
});

const ApiResourcePicker: React.FC<ApiResourcePickerProps> = ({
  visible,
  currentPermission,
  onClose,
  onConfirm,
}) => {
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<ApiResourceItem[]>([]);
  const [total, setTotal] = useState(0);
  const [selectedMap, setSelectedMap] = useState<Map<string, ApiResourceItem>>(
    new Map(),
  );
  const [mainApiKey, setMainApiKey] = useState<string | null>(null);
  const [query, setQuery] = useState<ApiResourceQuery>({
    pageNum: 1,
    pageSize: 10,
  });
  const [searchModulePrefix, setSearchModulePrefix] = useState<
    string | undefined
  >(undefined);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [moduleOptions, setModuleOptions] = useState<ModuleInfo[]>([]);

  // 加载模块列表
  useEffect(() => {
    if (visible) {
      getModuleList().then(setModuleOptions).catch(() => {});
    }
  }, [visible]);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getApiResourcePage({
        ...query,
        modulePrefix: searchModulePrefix || undefined,
        keyword: searchKeyword || undefined,
      });
      setDataSource(result.records);
      setTotal(result.total);
    } catch {
      message.error('加载接口资源失败');
    } finally {
      setLoading(false);
    }
  }, [query, searchModulePrefix, searchKeyword]);

  useEffect(() => {
    if (visible) {
      loadData();
    }
  }, [visible, loadData]);

  // 打开弹窗时，解析当前权限恢复选中状态和主接口
  useEffect(() => {
    if (visible) {
      if (currentPermission) {
        const parsed = parsePermissionString(currentPermission);
        const newMap = new Map<string, ApiResourceItem>();
        let mainKey: string | null = null;
        for (const item of parsed) {
          newMap.set(item.key, createItemFromParsed(item));
          if (item.isMain) {
            mainKey = item.key;
          }
        }
        setSelectedMap(newMap);
        setMainApiKey(mainKey);
      } else {
        setSelectedMap(new Map());
        setMainApiKey(null);
      }
      // 重置搜索条件
      setSearchModulePrefix(undefined);
      setSearchKeyword('');
      setQuery({ pageNum: 1, pageSize: 10 });
    }
  }, [visible, currentPermission]);

  // 当表格数据加载后，用完整数据补充已选项（回显时可能只有最小信息）
  useEffect(() => {
    if (dataSource.length === 0) return;
    setSelectedMap((prev) => {
      let changed = false;
      const newMap = new Map(prev);
      for (const item of dataSource) {
        const key = buildPermKey(item);
        if (newMap.has(key)) {
          const existing = newMap.get(key)!;
          if (existing.id === key || !existing.summary) {
            newMap.set(key, item);
            changed = true;
          }
        }
      }
      return changed ? newMap : prev;
    });
  }, [dataSource]);

  const handleSearch = useCallback(() => {
    setQuery((prev) => ({ ...prev, pageNum: 1 }));
  }, []);

  // 从列表添加接口到已选
  const handleAddItem = useCallback((item: ApiResourceItem) => {
    const key = buildPermKey(item);
    setSelectedMap((prev) => {
      if (prev.has(key)) return prev;
      const newMap = new Map(prev);
      newMap.set(key, item);
      return newMap;
    });
  }, []);

  // 从已选移除接口
  const handleRemoveItem = useCallback((key: string) => {
    setSelectedMap((prev) => {
      const newMap = new Map(prev);
      newMap.delete(key);
      return newMap;
    });
    setMainApiKey((prev) => (prev === key ? null : prev));
  }, []);

  const selectedItems = Array.from(selectedMap.entries());
  const selectedCount = selectedMap.size;

  // 生成权限标识字符串
  const buildPermissionString = useCallback((): string => {
    if (selectedCount === 0) return '';
    const effectiveMainKey =
      selectedCount === 1 ? selectedItems[0][0] : mainApiKey;
    return selectedItems
      .map(([key]) =>
        effectiveMainKey && key === effectiveMainKey
          ? `(main)${key}`
          : key,
      )
      .join(';');
  }, [selectedItems, mainApiKey, selectedCount]);

  const permissionPreview = buildPermissionString();

  const columns: TableProps<ApiResourceItem>['columns'] = [
    {
      title: '请求方式',
      dataIndex: 'reqMethod',
      width: 90,
      render: (val: string) => (
        <Tag color={METHOD_COLORS[val] || 'default'}>{val}</Tag>
      ),
    },
    {
      title: '模块前缀',
      dataIndex: 'modulePrefix',
      width: 100,
    },
    {
      title: '接口地址',
      dataIndex: 'reqPath',
      ellipsis: true,
    },
    {
      title: '摘要',
      dataIndex: 'summary',
      ellipsis: true,
    },
    {
      title: '操作',
      width: 60,
      align: 'center',
      render: (_: unknown, record: ApiResourceItem) => {
        const key = buildPermKey(record);
        return selectedMap.has(key) ? (
          <Tag color="blue">已选</Tag>
        ) : (
          <Button
            type="link"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => handleAddItem(record)}
            title="添加到已选"
          />
        );
      },
    },
  ];

  const handleConfirm = useCallback(() => {
    onConfirm(permissionPreview);
    onClose();
  }, [permissionPreview, onConfirm, onClose]);

  return (
    <Modal
      title="选择接口资源"
      open={visible}
      width={800}
      onOk={handleConfirm}
      onCancel={onClose}
      okButtonProps={{ disabled: selectedCount === 0 }}
      className={styles.pickerModal}
    >
      <div className={styles.searchBar}>
        <Select
          className={styles.searchSelect}
          placeholder="模块前缀"
          value={searchModulePrefix}
          onChange={(val) => {
            setSearchModulePrefix(val);
            setQuery((prev) => ({ ...prev, pageNum: 1 }));
          }}
          allowClear
          options={moduleOptions.map((m) => ({
            value: m.prefix,
            label: `${m.prefix}（${m.note}）`,
          }))}
        />
        <Input
          className={styles.searchInput}
          placeholder="搜索Tag名称/接口地址/摘要"
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          onPressEnter={handleSearch}
          allowClear
        />
        <Button
          icon={<SearchOutlined />}
          type="primary"
          onClick={handleSearch}
        >
          搜索
        </Button>
      </div>
      <div className={styles.tableWrapper}>
        <Table<ApiResourceItem>
          rowKey="id"
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          size="small"
          pagination={{
            current: query.pageNum,
            pageSize: query.pageSize,
            total,
            size: 'small',
            onChange: (page, pageSize) =>
              setQuery({ pageNum: page, pageSize }),
          }}
        />
      </div>
      {/* 已选接口区域 */}
      <div className={styles.selectedArea}>
        <div className={styles.selectedAreaLabel}>
          已选接口（{selectedCount}）
        </div>
        {selectedCount > 0 ? (
          <div className={styles.selectedTags}>
            {selectedItems.map(([key, item]) => {
              const isMain =
                selectedCount > 1 && key === mainApiKey;
              return (
                <Tag
                  key={key}
                  color={METHOD_COLORS[item.reqMethod] || 'default'}
                  closable
                  onClose={(e) => {
                    e.preventDefault();
                    handleRemoveItem(key);
                  }}
                >
                  {isMain && (
                    <span style={{ fontWeight: 600, marginRight: 2 }}>
                      (main)
                    </span>
                  )}
                  {item.reqMethod}:{item.modulePrefix}:{item.reqPath}
                </Tag>
              );
            })}
          </div>
        ) : (
          <div className={styles.selectedEmpty}>
            请从上方列表中选择接口
          </div>
        )}
      </div>
      {/* 权限标识预览 */}
      {selectedCount > 0 && (
        <div className={styles.permissionPreview}>
          <div className={styles.permissionPreviewLabel}>
            权限标识预览：
          </div>
          <div className={styles.permissionPreviewText}>
            {permissionPreview}
          </div>
          {/* 主接口选择 */}
          {selectedCount > 1 && (
            <div className={styles.mainApiSection}>
              <div className={styles.mainApiLabel}>
                主接口
                <Tooltip title="主接口是该功能最核心的接口。例如新增功能配置了新增接口和获取字典选项接口，则新增接口为主接口。主接口标识将用于后续AI分析和权限判断。">
                  <QuestionCircleOutlined
                    style={{
                      marginLeft: 4,
                      color: 'var(--text-secondary-color)',
                    }}
                  />
                </Tooltip>
              </div>
              <Radio.Group
                value={mainApiKey}
                onChange={(e) => setMainApiKey(e.target.value)}
                className={styles.mainApiRadioGroup}
              >
                {selectedItems.map(([key, item]) => (
                  <Radio
                    key={key}
                    value={key}
                    className={styles.mainApiRadio}
                  >
                    <Tag
                      color={
                        METHOD_COLORS[item.reqMethod] || 'default'
                      }
                      style={{ marginRight: 4 }}
                    >
                      {item.reqMethod}
                    </Tag>
                    {item.modulePrefix}:{item.reqPath}
                    {item.summary && (
                      <span
                        style={{
                          color: 'var(--text-secondary-color)',
                          marginLeft: 4,
                        }}
                      >
                        ({item.summary})
                      </span>
                    )}
                  </Radio>
                ))}
              </Radio.Group>
            </div>
          )}
          {selectedCount === 1 && (
            <div className={styles.mainApiHint}>
              仅选择了一个接口，该接口自动作为主接口
            </div>
          )}
        </div>
      )}
    </Modal>
  );
};

export default ApiResourcePicker;
