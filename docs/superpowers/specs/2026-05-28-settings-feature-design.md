# 设置功能设计文档

## 概述

为主应用（gwsu-main）添加设置功能，支持配置管理和字典管理。设置入口在右上角用户下拉面板的设置按钮，点击后在助手操作区显示设置面板。

## 需求摘要

1. **配置（security_config）**：键值对，值可以是基本类型或 JSON 对象
2. **字典（security_dict）**：键 → 值列表，值只能是基本类型
3. 配置和字典分为**系统**（出厂预设）和**自定义**两种类型
4. 系统类型不可删除，自定义类型支持增删改查
5. 设置面板内部使用三 Tab 布局，支持 Tab 滚动

## 前端设计

### 入口与切换

- **入口**：右上角用户下拉面板中的设置按钮
- **展示**：替换助手操作区内容，与 AI 输出、界面 Tab 互斥
- **销毁策略**：设置面板切换走时销毁，不保持状态

### 操作区可扩展架构

将 `AssistantOperationArea` 改为注册式组件架构：

```typescript
interface OperationTabConfig {
  key: string;                              // Tab 唯一标识
  label: string;                            // Tab 显示名称
  component: React.LazyExoticComponent<any>; // 懒加载组件
  keepAlive: boolean;                       // 切换走时是否保持存活
  showInHeader?: boolean;                   // 是否在顶部导航栏显示 Tab
}

const operationTabRegistry: OperationTabConfig[] = [
  { key: 'interface', label: '界面', component: InterfaceOperation, keepAlive: false, showInHeader: true },
  { key: 'ai-output', label: 'AI 输出', component: AiOutputPanel, keepAlive: true, showInHeader: true },
  { key: 'settings', label: '设置', component: SettingsPanel, keepAlive: false, showInHeader: false },
];
```

- `keepAlive: true` → 使用 `display: none` 隐藏（如 AI 输出）
- `keepAlive: false` → 条件渲染，切换走即销毁（如界面、设置）
- `showInHeader: false` → 不在导航栏显示，通过其他入口触发

`OperationTab` 类型扩展为：`'interface' | 'ai-output' | 'settings'`

新增 `switchToSettings()` 方法到 `useOperationTabStore`。

### 设置面板组件结构

```
SettingsPanel/
├── index.tsx                    # 主容器，Ant Design Tabs（可滚动）
├── index.module.less
├── AssistantConfigTab/          # 助手配置 Tab
│   ├── index.tsx                # 系统配置分页表格
│   ├── index.module.less
│   ├── ConfigFormModal.tsx      # 专用表单弹窗（根据 configKey 渲染不同表单）
│   └── types.ts
├── DictConfigTab/               # 字典配置 Tab
│   ├── index.tsx                # 左右分栏主组件
│   ├── index.module.less
│   ├── DictKeyList.tsx          # 左侧：字典键列表
│   ├── DictValueList.tsx        # 右侧：值列表编辑
│   └── types.ts
└── CustomConfigTab/             # 其他配置 Tab
    ├── index.tsx                # 自定义配置分页表格 + 增删改查
    ├── index.module.less
    ├── CustomConfigFormModal.tsx # 新增/编辑弹窗
    └── types.ts
```

### 助手配置 Tab

- 分页表格展示所有系统类型配置
- 列：配置键、配置名称、描述、更新时间、操作
- 操作：编辑按钮（系统配置不可删除）
- 点击编辑 → ConfigFormModal
  - 根据 `configKey` 渲染不同的专用表单
  - `assistant_config` → 助手相关配置表单
  - 保存时将表单数据序列化为 JSON 存入 `configValue`

### 字典配置 Tab（左右分栏）

**左侧 — 字典键列表**：
- 分页表格，列：字典键、字典名称、描述、值数量、操作
- 操作：编辑（修改 dictKey/名称/描述）、删除
- 新增按钮（新增的都是自定义类型）

**右侧 — 值列表编辑**：
- 选中左侧某条字典后，右侧展示该字典下的值列表
- 可编辑表格，每个值项：序号、值、操作（编辑、删除、上移、下移）
- 新增值按钮

### 其他配置 Tab

- 分页表格展示所有自定义类型配置
- 列：配置键、配置名称、配置值、描述、更新时间、操作
- 操作：查看详情、编辑、删除
- 新增按钮 → CustomConfigFormModal
  - 字段：configKey、configName、configValue（基本类型输入框 / JSON 编辑器切换）、description
  - 所有新增配置都是自定义类型

### Tabs 可滚动

设置面板的 Ant Design Tabs 组件设置 `tabBarGutter`，CSS 添加 `overflow-x: auto`，确保 Tab 过多时横向滚动。

## 后端设计

### 数据库表

#### security_config

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(36) | 主键 |
| config_key | varchar(100) | 配置键，唯一索引 |
| config_name | varchar(100) | 配置名称 |
| config_value | text | 配置值（JSON 字符串或基本类型值） |
| value_type | tinyint | 值类型：1-基本类型，2-JSON |
| config_type | tinyint | 配置类型：1-系统，2-自定义 |
| description | varchar(500) | 描述 |
| module_prefix | varchar(50) | 所属模块前缀（默认 security） |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

#### security_dict

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(36) | 主键 |
| dict_key | varchar(100) | 字典键，唯一索引 |
| dict_name | varchar(100) | 字典名称 |
| dict_type | tinyint | 字典类型：1-系统，2-自定义 |
| description | varchar(500) | 描述 |
| module_prefix | varchar(50) | 所属模块前缀 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

#### security_dict_value

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(36) | 主键 |
| dict_id | varchar(36) | 关联字典ID |
| dict_value | varchar(500) | 字典值（基本类型） |
| sort | int | 排序序号 |
| create_time | datetime | 创建时间 |

### 后端模块结构

遵循现有 business-security 模块的分层结构：

```
business-security-server/src/main/java/org/quyq/gwsu/security/
├── config/
│   ├── controller/
│   │   └── SecurityConfigController.java
│   ├── domain/
│   │   ├── SecurityConfig.java
│   │   └── SecurityDictValue.java  (embedded in dict)
│   ├── mapper/
│   │   └── SecurityConfigMapper.java
│   └── service/
│       ├── ISecurityConfigService.java
│       └── impl/
│           └── SecurityConfigServiceImpl.java
└── dict/
    ├── controller/
    │   ├── SecurityDictController.java
    │   └── SecurityDictValueController.java
    ├── domain/
    │   ├── SecurityDict.java
    │   └── SecurityDictValue.java
    ├── mapper/
    │   ├── SecurityDictMapper.java
    │   └── SecurityDictValueMapper.java
    └── service/
        ├── ISecurityDictService.java
        ├── ISecurityDictValueService.java
        └── impl/
            ├── SecurityDictServiceImpl.java
            └── SecurityDictValueServiceImpl.java
```

API 模块：

```
business-security-api/src/main/java/org/quyq/gwsu/security/api/
├── config/
│   ├── SecurityConfigClientApi.java
│   ├── dto/
│   │   ├── ConfigQueryDTO.java
│   │   └── ConfigSaveDTO.java
│   ├── fallback/
│   │   └── SecurityConfigClientApiFallbackFactory.java
│   └── vo/
│       └── ConfigVO.java
└── dict/
    ├── SecurityDictClientApi.java
    ├── dto/
    │   ├── DictQueryDTO.java
    │   ├── DictSaveDTO.java
    │   └── DictValueSaveDTO.java
    ├── fallback/
    │   └── SecurityDictClientApiFallbackFactory.java
    └── vo/
        ├── DictVO.java
        └── DictValueVO.java
```

### API 设计

#### 配置 API（/security/config）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /security/config/page | 分页查询配置 |
| GET | /security/config/{id} | 根据ID查询配置 |
| GET | /security/config/key/{configKey} | 根据键查询配置 |
| POST | /security/config | 新增配置（仅自定义类型） |
| PUT | /security/config | 更新配置 |
| DELETE | /security/config | 批量删除配置（仅自定义类型） |

#### 字典 API（/security/dict）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /security/dict/page | 分页查询字典 |
| GET | /security/dict/{id} | 根据ID查询字典 |
| POST | /security/dict | 新增字典（仅自定义类型） |
| PUT | /security/dict | 更新字典 |
| DELETE | /security/dict | 批量删除字典（仅自定义类型） |

#### 字典值 API（/security/dict-value）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /security/dict-value/list/{dictId} | 查询字典下的值列表 |
| POST | /security/dict-value | 新增字典值 |
| PUT | /security/dict-value | 更新字典值 |
| DELETE | /security/dict-value | 批量删除字典值 |
| PUT | /security/dict-value/sort | 更新排序 |

### 业务规则

1. 系统类型（config_type=1 / dict_type=1）的配置和字典不可删除、不可修改键名
2. 新增的配置和字典默认为自定义类型（config_type=2 / dict_type=2）
3. 配置的 config_key 和字典的 dict_key 在同模块内唯一
4. 字典值只能是基本类型，配置值可以是基本类型或 JSON
5. 删除字典时级联删除其下所有值
6. module_prefix 用于多模块隔离，默认为 security
