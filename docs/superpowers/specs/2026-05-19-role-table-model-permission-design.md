# 角色表模型权限配置 - 设计文档

## 概述

在角色管理页面，点击"表模型权限"操作项，弹出左右分栏的配置弹窗，展示和配置该角色的表模型权限信息。主要配置哪些表、哪些字段允许查询、哪些字段需要脱敏及脱敏策略。

## 数据模型

### 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/security/roleTableModel/getTableModelPermission/{roleId}` | 获取角色表模型权限列表 |
| POST | `/security/roleTableModel` | 保存或更新角色表模型权限 |
| DELETE | `/security/roleTableModel` | 批量删除，body 为 id 列表 |

### 响应结构 (RolePermissionTableModelVO)

```typescript
interface RolePermissionTableModelVO {
  type: number;           // 0-接口关联；1-角色自定义配置
  tableModelId: string;   // 表模型ID
  id?: string;            // security_role_table_model 主键，仅 type=1 时有值
  modulePrefix: string;   // 所属模块
  datasource: string;     // 数据源
  tableName: string;      // 表名
  tableComment: string;   // 表注释
  columns: ColumnInfo[];
}

interface ColumnInfo {
  columnName: string;
  columnComment: string;
  fixedFieldConfig?: FieldConfigItem;   // 不可变配置（注解采集），有值则锁定
  customFieldConfig?: FieldConfigItem;  // 自定义配置，用户可编辑
}

interface FieldConfigItem {
  fieldName: string;
  show: boolean;           // 是否允许查询
  desensitize: boolean;    // 是否脱敏
  strategy: string;        // 脱敏策略: NONE/USERNAME/ID_CARD/PHONE/EMAIL/ADDRESS/CUSTOM
  prefixNoMaskLen?: number; // 自定义-不脱敏前缀长度
  suffixNoMaskLen?: number; // 自定义-不脱敏后缀长度
  symbol?: string;         // 自定义-脱敏标识符
}
```

### 保存请求 (RoleTableModelSaveDTO)

```typescript
interface RoleTableModelSaveDTO {
  id?: string;            // 主键ID（更新时传入）
  roleId: string;
  modulePrefix: string;
  tableName: string;
  datasource: string;
  fields: FieldConfigItem[]; // 只传可编辑字段的配置（排除 fixedFieldConfig 锁定的字段）
}
```

### 核心规则

1. **fixedFieldConfig 与 customFieldConfig 互斥**：fixedFieldConfig 有值时，该字段锁定不可修改，customFieldConfig 不会存在
2. **锁定字段**：fixedFieldConfig 有值的字段 → 回显值 + 禁止修改 + 保存时不传
3. **id 字段含义**：有值表示该表有自定义配置数据；无值表示权限来自接口自动映射
4. **type 字段含义**：0=接口关联权限，1=角色自定义配置
5. **默认值**：无 fixed 也无 custom 的字段，默认 show=true, desensitize=false, strategy=NONE

## 界面设计

### 弹窗整体

- 宽度 1080px，高度约 700px
- 左右分栏：左侧 280px（表列表），右侧自适应（字段配置）
- 标题：`表模型权限配置 - {角色名}`
- 底部关闭按钮

### 左侧面板 - 表列表

| 元素 | 说明 |
|------|------|
| 标题栏 | "表模型列表" + "新增模型权限"按钮 |
| 搜索框 | 表名模糊搜索，过滤左侧列表 |
| 列表项 | 显示 `表注释(表名)`，选中项蓝色左边框高亮 |
| 类型标签 | type=0 显示"接口关联"标签(蓝)，type=1 显示"自定义配置"标签(绿) |
| 删除按钮 | hover 显示，仅 type=1 时可删除，确认后调用 DELETE 接口 |

### 右侧面板 - 字段权限配置

#### 表信息区

显示：表注释(表名)、模块、数据源。若 id 有值显示"已自定义配置"标签。

#### 字段配置表格

| 列 | 宽度 | 说明 |
|------|------|------|
| 字段名 | 140px | columnName |
| 字段注释 | 160px | columnComment |
| 允许查询 | 90px | Switch，控制 show |
| 是否脱敏 | 90px | Switch，控制 desensitize |
| 脱敏策略 | 150px | Select，策略枚举 |
| 自定义参数 | 自适应 | 仅 strategy=CUSTOM 时显示：前缀保留、后缀保留、脱敏符号 |

**锁定行样式**：
- fixedFieldConfig 有值的行 → 浅灰背景 + 锁图标 + Tooltip "默认配置，禁止修改"
- 所有 Switch/Select disabled，回显 fixedFieldConfig 的值

**默认值**：无 fixed 也无 custom → show=true, desensitize=false, strategy=NONE

**联动逻辑**：
- 脱敏开关关闭时，脱敏策略列置灰
- 脱敏策略选择 CUSTOM 时，展开自定义参数输入框

#### 底部操作栏

- 保存：收集可编辑字段配置，调用 POST 接口
- 重置：恢复到接口返回的初始数据

### 新增模型权限弹框

- 点击"新增模型权限"按钮弹出小弹框（400px 宽）
- 搜索框：模糊搜索 security_tablemodel_tables，排除已有表
- 勾选后点确认，批量调用 POST 接口创建默认权限
- 创建成功后刷新左侧列表

## 组件结构

```
role/components/
├── TableModelPermissionModal/
│   ├── index.tsx                    # 主弹窗组件
│   ├── index.module.less            # 样式
│   ├── TableListPanel.tsx           # 左侧表列表面板
│   ├── FieldConfigTable.tsx         # 右侧字段配置表格
│   ├── AddTableModelModal.tsx       # 新增模型权限弹框
│   ├── types.ts                     # 类型定义
│   └── hooks/
│       └── useTableModelPermission.ts  # 数据逻辑 hook
```

## 后端接口补充

当前缺少一个搜索 `security_tablemodel_tables` 的接口用于"新增模型权限"。需确认是否复用已有的表模型分页查询接口（`/security/tablemodel/page`），或新增一个专门接口。

## 保存逻辑

1. 遍历当前选中表的所有字段
2. 过滤掉 fixedFieldConfig 有值的字段（锁定字段不传）
3. 收集可编辑字段的当前配置值
4. 构建 RoleTableModelSaveDTO，包含 roleId、modulePrefix、tableName、datasource、fields
5. 若 id 有值则传入（更新），无值则不传（新增）
6. 调用 POST /security/roleTableModel
