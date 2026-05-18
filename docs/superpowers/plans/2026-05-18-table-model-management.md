# 表模型管理界面 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现表模型管理界面，支持采集和自定义添加两种类型的表模型维护，包括字段结构查看/编辑、数据源修改、同步等核心功能。

**Architecture:** 后端在 SecurityTableModelTableController 中新增管理接口（分页查询、自定义添加、采集、同步、修改数据源、编辑字段/外键）。前端在 gwsu-sub-security 子应用中新建 tablemodel 页面，采用列表页模式（搜索栏 + 表格 + 弹窗），包含采集弹窗（步骤条）、自定义添加弹窗、详情抽屉、修改数据源弹窗、编辑字段/外键弹窗。

**Tech Stack:** Spring Boot 4 / MyBatis Plus（后端），UmiJS 4 + Ant Design 6 + React 18（前端）

---

## 文件结构

### 后端新增/修改文件

```
business-security/business-security-api/src/main/java/org/quyq/gwsu/security/api/tablemodel/
├── dto/
│   ├── TableModelTableQueryDTO.java      # 表模型分页查询DTO（新建）
│   ├── TableModelCollectDTO.java         # 采集请求DTO（新建）
│   ├── TableModelCustomSaveDTO.java      # 自定义添加DTO（新建）
│   ├── TableModelChangeDatasourceDTO.java # 修改数据源DTO（新建）
│   └── TableModelSyncDTO.java            # 同步请求DTO（新建）
└── vo/
    ├── TableModelTableVO.java            # 已存在，无需修改
    ├── TableModelDetailVO.java           # 已存在，无需修改
    ├── TableModelColumnVO.java           # 已存在，无需修改
    └── TableModelForeignKeyVO.java       # 已存在，无需修改

business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/
├── controller/
│   └── SecurityTableModelTableController.java  # 修改：新增6个接口
└── service/
    ├── ISecurityTableModelTableService.java    # 修改：新增6个方法签名
    └── impl/
        └── SecurityTableModelTableServiceImpl.java  # 修改：实现6个新方法
```

### 前端新增/修改文件

```
web/apps/gwsu-sub-security/
├── config/
│   └── routes.ts                              # 修改：添加 /tablemodel 路由
└── src/pages/tablemodel/
    ├── index.tsx                               # 主列表页
    ├── index.module.less                       # 主列表页样式
    ├── types/index.ts                          # 类型定义
    ├── permissionConstants.ts                  # 按钮权限常量
    ├── services/tableModel.ts                  # API 服务
    ├── hooks/useTableModel.ts                  # 列表页 Hook
    └── components/
        ├── CollectModal/                       # 采集弹窗（步骤条）
        │   ├── index.tsx
        │   └── index.module.less
        ├── CustomAddModal/                     # 自定义添加弹窗
        │   ├── index.tsx
        │   └── index.module.less
        ├── DetailDrawer/                       # 详情抽屉（含字段+外键表）
        │   ├── index.tsx
        │   └── index.module.less
        ├── ChangeDatasourceModal/              # 修改数据源弹窗
        │   ├── index.tsx
        │   └── index.module.less
        ├── ColumnEditModal/                    # 编辑字段弹窗
        │   ├── index.tsx
        │   └── index.module.less
        └── ForeignKeyEditModal/                # 编辑外键弹窗
            ├── index.tsx
            └── index.module.less
```

---

## Task 1: 后端 - 新增 DTO 类

**Files:**
- Create: `business-security/business-security-api/src/main/java/org/quyq/gwsu/security/api/tablemodel/dto/TableModelTableQueryDTO.java`
- Create: `business-security/business-security-api/src/main/java/org/quyq/gwsu/security/api/tablemodel/dto/TableModelCollectDTO.java`
- Create: `business-security/business-security-api/src/main/java/org/quyq/gwsu/security/api/tablemodel/dto/TableModelCustomSaveDTO.java`
- Create: `business-security/business-security-api/src/main/java/org/quyq/gwsu/security/api/tablemodel/dto/TableModelChangeDatasourceDTO.java`

- [ ] **Step 1: 创建 TableModelTableQueryDTO**

```java
package org.quyq.gwsu.security.api.tablemodel.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

@EqualsAndHashCode(callSuper = true)
@Data
public class TableModelTableQueryDTO extends BaseDTO {
    private String modulePrefix;
    private String tableName;
    private String dataSource;
    private Integer sourceType;
}
```

- [ ] **Step 2: 创建 TableModelCollectDTO**

```java
package org.quyq.gwsu.security.api.tablemodel.dto;

import java.util.List;

public record TableModelCollectDTO(
        List<TableModelCollectItem> items
) {
    public record TableModelCollectItem(
            String modulePrefix,
            String datasource,
            String tableName
    ) {}
}
```

- [ ] **Step 3: 创建 TableModelCustomSaveDTO**

```java
package org.quyq.gwsu.security.api.tablemodel.dto;

public record TableModelCustomSaveDTO(
        String applicationName,
        String modulePrefix,
        String datasource,
        String tableName
) {}
```

- [ ] **Step 4: 创建 TableModelChangeDatasourceDTO**

```java
package org.quyq.gwsu.security.api.tablemodel.dto;

import java.util.List;

public record TableModelChangeDatasourceDTO(
        String tableModelId,
        String newDatasource,
        List<String> apiIds
) {}
```

- [ ] **Step 5: Commit**

```bash
git add business-security/business-security-api/src/main/java/org/quyq/gwsu/security/api/tablemodel/dto/
git commit -m "feat: 新增表模型管理 DTO 类"
```

---

## Task 2: 后端 - Service 层新增方法

**Files:**
- Modify: `business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/service/ISecurityTableModelTableService.java`
- Modify: `business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/service/impl/SecurityTableModelTableServiceImpl.java`

- [ ] **Step 1: 在 ISecurityTableModelTableService 中新增方法签名**

在接口中新增以下方法：

```java
/** 分页查询表模型列表 */
IPage<TableModelTableVO> pageByCondition(TableModelTableQueryDTO query);

/** 查询未采集的表模型列表（从api_table_model中分析） */
List<TableModelTableVO> listUncollected(String modulePrefix);

/** 采集表模型（批量保存表+字段+外键） */
Boolean collectTableModels(TableModelCollectDTO dto);

/** 自定义添加表模型 */
TableModelTableVO customSave(TableModelCustomSaveDTO dto);

/** 同步表模型字段（与库中最新对比，增删字段） */
Boolean syncTableModel(String tableModelId);

/** 修改数据源 */
Boolean changeDatasource(TableModelChangeDatasourceDTO dto);
```

- [ ] **Step 2: 在 SecurityTableModelTableServiceImpl 中实现 pageByCondition**

```java
@Override
public IPage<TableModelTableVO> pageByCondition(TableModelTableQueryDTO query) {
    Page<SecurityTableModelTable> page = new Page<>(query.getPageNum(), query.getPageSize());
    LambdaQueryWrapper<SecurityTableModelTable> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(SecurityTableModelTable::getDeleted, false);
    if (StringUtils.isNotBlank(query.getModulePrefix())) {
        wrapper.eq(SecurityTableModelTable::getModulePrefix, query.getModulePrefix());
    }
    if (StringUtils.isNotBlank(query.getTableName())) {
        wrapper.like(SecurityTableModelTable::getTableName, query.getTableName());
    }
    if (StringUtils.isNotBlank(query.getDataSource())) {
        wrapper.eq(SecurityTableModelTable::getDataSource, query.getDataSource());
    }
    if (query.getSourceType() != null) {
        wrapper.eq(SecurityTableModelTable::getSourceType, query.getSourceType());
    }
    wrapper.orderByDesc(SecurityTableModelTable::getCreateTime);
    return page(page, wrapper).convert(SecurityTableModelTable::toVo);
}
```

- [ ] **Step 3: 在 SecurityTableModelTableServiceImpl 中实现 listUncollected**

逻辑：从 `security_api_table_model` 中按 modulePrefix 分组获取 (modulePrefix, datasource, tableName) 组合，再与 `security_tablemodel_tables` 中已有记录做差集。

```java
@Override
public List<TableModelTableVO> listUncollected(String modulePrefix) {
    // 查询指定模块的 api_table_model 绑定记录
    List<SecurityApiTableModel> apiTableModels = apiTableModelService.lambdaQuery()
            .eq(SecurityApiTableModel::getModulePrefix, modulePrefix)
            .list();

    // 提取唯一的 (modulePrefix, datasource, tableName) 组合
    Set<String> apiModelKeys = apiTableModels.stream()
            .map(m -> "%s:%s:%s".formatted(m.getModulePrefix(), m.getDatasource(), m.getTableName()))
            .collect(Collectors.toSet());

    // 查询已采集的表模型
    List<SecurityTableModelTable> existing = list(new LambdaQueryWrapper<SecurityTableModelTable>()
            .eq(SecurityTableModelTable::getModulePrefix, modulePrefix)
            .eq(SecurityTableModelTable::getDeleted, false));
    Set<String> existingKeys = existing.stream()
            .map(t -> "%s:%s:%s".formatted(t.getModulePrefix(), t.getDataSource(), t.getTableName()))
            .collect(Collectors.toSet());

    // 差集 = 未采集
    apiModelKeys.removeAll(existingKeys);

    // 构造未采集列表VO（从 api_table_model 中提取信息）
    return apiTableModels.stream()
            .filter(m -> apiModelKeys.contains("%s:%s:%s".formatted(m.getModulePrefix(), m.getDatasource(), m.getTableName())))
            .collect(Collectors.groupingBy(m -> "%s:%s:%s".formatted(m.getModulePrefix(), m.getDatasource(), m.getTableName())))
            .values().stream()
            .map(list -> list.get(0))
            .map(m -> {
                TableModelTableVO vo = new TableModelTableVO();
                vo.setModulePrefix(m.getModulePrefix());
                vo.setDataSource(m.getDatasource());
                vo.setTableName(m.getTableName());
                vo.setSourceType(0);
                return vo;
            })
            .toList();
}
```

注意：需要在类中注入 `ISecurityApiTableModelService apiTableModelService`。

- [ ] **Step 4: 在 SecurityTableModelTableServiceImpl 中实现 collectTableModels**

```java
@Override
@Transactional
public Boolean collectTableModels(TableModelCollectDTO dto) {
    for (TableModelCollectDTO.TableModelCollectItem item : dto.items()) {
        // 检查是否已存在
        long count = count(new LambdaQueryWrapper<SecurityTableModelTable>()
                .eq(SecurityTableModelTable::getModulePrefix, item.modulePrefix())
                .eq(SecurityTableModelTable::getDataSource, item.datasource())
                .eq(SecurityTableModelTable::getTableName, item.tableName())
                .eq(SecurityTableModelTable::getDeleted, false));
        if (count > 0) continue;

        // 创建表记录
        SecurityTableModelTable table = new SecurityTableModelTable();
        table.setTableName(item.tableName());
        table.setModulePrefix(item.modulePrefix());
        table.setDataSource(item.datasource());
        table.setSourceType(0);
        save(table);

        // 从库中获取字段信息并保存
        String applicationName = getApplicationName(item.modulePrefix());
        List<ColumnInfo> columns = columnList(applicationName, item.datasource(), item.tableName());
        if (!CollectionUtils.isEmpty(columns)) {
            List<SecurityTableModelColumn> columnEntities = columns.stream().map(c -> {
                SecurityTableModelColumn col = new SecurityTableModelColumn();
                col.setTableId(table.getId());
                col.setColumnName(c.getName());
                col.setColumnType(c.getType());
                col.setColumnLength(c.getLength());
                col.setColumnScale(c.getScale());
                col.setIsNullable(c.getNullable());
                col.setIsPrimaryKey(c.getIsPrimaryKey());
                col.setColumnComment(c.getRemark());
                col.setOrdinalPosition(c.getPosition());
                col.setDefaultValue(c.getDefaultValue());
                return col;
            }).toList();
            securityTableModelColumnService.saveBatch(columnEntities);
        }

        // 从库中获取外键信息并保存（需通过SQL获取外键，此处调用 metadata 接口）
        saveForeignKeysFromDb(table.getId(), applicationName, item.datasource(), item.tableName());
    }
    return true;
}
```

需要新增辅助方法 `getApplicationName(String modulePrefix)` 和 `saveForeignKeysFromDb(...)`。

`getApplicationName` 通过注入 `List<BusinessModuleInfoProvider>` 遍历查找：
```java
private String getApplicationName(String modulePrefix) {
    return providers.stream()
            .filter(p -> p.module().prefix().equals(modulePrefix))
            .map(BusinessModuleInfoProvider::applicationName)
            .findFirst()
            .orElse(modulePrefix);
}
```

`saveForeignKeysFromDb` 通过 `ISQLExecutionService` 或 REST 调用获取外键信息并保存。

- [ ] **Step 5: 在 SecurityTableModelTableServiceImpl 中实现 customSave**

```java
@Override
@Transactional
public TableModelTableVO customSave(TableModelCustomSaveDTO dto) {
    // 唯一性校验：modulePrefix + dataSource + tableName
    long count = count(new LambdaQueryWrapper<SecurityTableModelTable>()
            .eq(SecurityTableModelTable::getModulePrefix, dto.modulePrefix())
            .eq(SecurityTableModelTable::getDataSource, dto.datasource())
            .eq(SecurityTableModelTable::getTableName, dto.tableName())
            .eq(SecurityTableModelTable::getDeleted, false));
    if (count > 0) {
        throw new SecurityException(SecurityErrorCode.E03003); // 表模型已存在
    }

    // 创建表记录
    SecurityTableModelTable table = new SecurityTableModelTable();
    table.setTableName(dto.tableName());
    table.setModulePrefix(dto.modulePrefix());
    table.setDataSource(dto.datasource());
    table.setSourceType(1);
    save(table);

    // 从库中获取字段信息
    List<ColumnInfo> columns = columnList(dto.applicationName(), dto.datasource(), dto.tableName());
    if (!CollectionUtils.isEmpty(columns)) {
        List<SecurityTableModelColumn> columnEntities = columns.stream().map(c -> {
            SecurityTableModelColumn col = new SecurityTableModelColumn();
            col.setTableId(table.getId());
            col.setColumnName(c.getName());
            col.setColumnType(c.getType());
            col.setColumnLength(c.getLength());
            col.setColumnScale(c.getScale());
            col.setIsNullable(c.getNullable());
            col.setIsPrimaryKey(c.getIsPrimaryKey());
            col.setColumnComment(c.getRemark());
            col.setOrdinalPosition(c.getPosition());
            col.setDefaultValue(c.getDefaultValue());
            return col;
        }).toList();
        securityTableModelColumnService.saveBatch(columnEntities);
    }

    // 从库中获取外键信息并保存
    saveForeignKeysFromDb(table.getId(), dto.applicationName(), dto.datasource(), dto.tableName());

    return table.toVo();
}
```

- [ ] **Step 6: 在 SecurityTableModelTableServiceImpl 中实现 syncTableModel**

```java
@Override
@Transactional
public Boolean syncTableModel(String tableModelId) {
    SecurityTableModelTable table = super.getById(tableModelId);
    if (table == null) return false;

    String applicationName = getApplicationName(table.getModulePrefix());

    // 从库中获取最新字段
    List<ColumnInfo> latestColumns = columnList(applicationName, table.getDataSource(), table.getTableName());
    Map<String, ColumnInfo> latestMap = latestColumns.stream()
            .collect(Collectors.toMap(ColumnInfo::getName, c -> c, (a, b) -> a));

    // 查询已有字段
    List<SecurityTableModelColumn> existingColumns = securityTableModelColumnService.list(
            new LambdaQueryWrapper<SecurityTableModelColumn>()
                    .eq(SecurityTableModelColumn::getTableId, tableModelId)
                    .eq(SecurityTableModelColumn::getDeleted, false));
    Map<String, SecurityTableModelColumn> existingMap = existingColumns.stream()
            .collect(Collectors.toMap(SecurityTableModelColumn::getColumnName, c -> c, (a, b) -> a));

    // 找出需要新增的字段（库中有但tablemodel中没有的）
    List<String> toAdd = latestMap.keySet().stream()
            .filter(name -> !existingMap.containsKey(name))
            .toList();

    // 找出需要删除的字段（tablemodel中有但库中没有的）
    List<String> toRemove = existingMap.keySet().stream()
            .filter(name -> !latestMap.containsKey(name))
            .toList();

    // 新增字段
    if (!toAdd.isEmpty()) {
        List<SecurityTableModelColumn> newColumns = toAdd.stream().map(name -> {
            ColumnInfo c = latestMap.get(name);
            SecurityTableModelColumn col = new SecurityTableModelColumn();
            col.setTableId(tableModelId);
            col.setColumnName(c.getName());
            col.setColumnType(c.getType());
            col.setColumnLength(c.getLength());
            col.setColumnScale(c.getScale());
            col.setIsNullable(c.getNullable());
            col.setIsPrimaryKey(c.getIsPrimaryKey());
            col.setColumnComment(c.getRemark());
            col.setOrdinalPosition(c.getPosition());
            col.setDefaultValue(c.getDefaultValue());
            return col;
        }).toList();
        securityTableModelColumnService.saveBatch(newColumns);
    }

    // 删除字段（已有且未删除的字段不做修改，防止注释内容丢失）
    if (!toRemove.isEmpty()) {
        List<String> removeIds = toRemove.stream()
                .map(name -> existingMap.get(name).getId())
                .toList();
        securityTableModelColumnService.removeByIds(removeIds);
    }

    return true;
}
```

- [ ] **Step 7: 在 SecurityTableModelTableServiceImpl 中实现 changeDatasource**

这是最核心的逻辑，需注意：
1. 如果 apiIds 为空，表示修改所有关联接口的数据源
2. 如果 apiIds 不为空，只修改部分接口，需要复制数据
3. 处理 config 表时先删除旧数据再新增

```java
@Override
@Transactional
public Boolean changeDatasource(TableModelChangeDatasourceDTO dto) {
    SecurityTableModelTable table = super.getById(dto.tableModelId());
    if (table == null) return false;

    String modulePrefix = table.getModulePrefix();
    String oldDatasource = table.getDataSource();
    String newDatasource = dto.newDatasource();

    // 如果新旧数据源相同，无需修改
    if (oldDatasource.equals(newDatasource)) return true;

    // 检查新数据源的表模型是否已存在（唯一性校验：modulePrefix + dataSource + tableName）
    long existCount = count(new LambdaQueryWrapper<SecurityTableModelTable>()
            .eq(SecurityTableModelTable::getModulePrefix, modulePrefix)
            .eq(SecurityTableModelTable::getDataSource, newDatasource)
            .eq(SecurityTableModelTable::getTableName, table.getTableName())
            .eq(SecurityTableModelTable::getDeleted, false));

    // 获取该表模型关联的所有 api_table_model 记录
    List<SecurityApiTableModel> relatedBindings = apiTableModelService.lambdaQuery()
            .eq(SecurityApiTableModel::getModulePrefix, modulePrefix)
            .eq(SecurityApiTableModel::getTableName, table.getTableName())
            .eq(SecurityApiTableModel::getDatasource, oldDatasource)
            .list();

    if (dto.apiIds() == null || dto.apiIds().isEmpty()) {
        // 场景1：修改所有接口的数据源
        // 直接修改 tablemodel_tables 的 dataSource
        table.setDataSource(newDatasource);
        updateById(table);

        // 为所有关联的 api_table_model 添加 config 记录
        for (SecurityApiTableModel binding : relatedBindings) {
            // 先删除旧的 config
            configMapper.delete(new LambdaQueryWrapper<SecurityApiTableModelConfig>()
                    .eq(SecurityApiTableModelConfig::getTableModelId, binding.getId()));
            // 新增 config
            SecurityApiTableModelConfig config = new SecurityApiTableModelConfig();
            config.setTableModelId(binding.getId());
            config.setDatasource(newDatasource);
            configMapper.insert(config);
        }
    } else {
        // 场景2：只修改部分接口
        // 原始 tablemodel_tables 不变（因为还有其他接口在用）
        // 需要创建新的 tablemodel_tables + columns + foreign_keys（如果不存在）
        String newTableId;
        if (existCount > 0) {
            // 新数据源的表模型已存在，获取其ID
            SecurityTableModelTable existingNew = getOne(new LambdaQueryWrapper<SecurityTableModelTable>()
                    .eq(SecurityTableModelTable::getModulePrefix, modulePrefix)
                    .eq(SecurityTableModelTable::getDataSource, newDatasource)
                    .eq(SecurityTableModelTable::getTableName, table.getTableName())
                    .eq(SecurityTableModelTable::getDeleted, false));
            newTableId = existingNew.getId();
        } else {
            // 复制原表模型数据
            SecurityTableModelTable newTable = new SecurityTableModelTable();
            newTable.setTableName(table.getTableName());
            newTable.setModulePrefix(modulePrefix);
            newTable.setDataSource(newDatasource);
            newTable.setTableComment(table.getTableComment());
            newTable.setSourceType(table.getSourceType());
            save(newTable);
            newTableId = newTable.getId();

            // 复制字段
            List<SecurityTableModelColumn> oldColumns = securityTableModelColumnService.list(
                    new LambdaQueryWrapper<SecurityTableModelColumn>()
                            .eq(SecurityTableModelColumn::getTableId, dto.tableModelId())
                            .eq(SecurityTableModelColumn::getDeleted, false));
            List<SecurityTableModelColumn> newColumns = oldColumns.stream().map(c -> {
                SecurityTableModelColumn col = new SecurityTableModelColumn();
                col.setTableId(newTableId);
                col.setColumnName(c.getColumnName());
                col.setColumnType(c.getColumnType());
                col.setColumnLength(c.getColumnLength());
                col.setColumnScale(c.getColumnScale());
                col.setIsNullable(c.getIsNullable());
                col.setIsPrimaryKey(c.getIsPrimaryKey());
                col.setPkPosition(c.getPkPosition());
                col.setDefaultValue(c.getDefaultValue());
                col.setColumnComment(c.getColumnComment());
                col.setOrdinalPosition(c.getOrdinalPosition());
                return col;
            }).toList();
            securityTableModelColumnService.saveBatch(newColumns);

            // 复制外键
            List<SecurityTableModelForeignKey> oldFks = securityTableModelForeignKeyService.list(
                    new LambdaQueryWrapper<SecurityTableModelForeignKey>()
                            .eq(SecurityTableModelForeignKey::getTableId, dto.tableModelId())
                            .eq(SecurityTableModelForeignKey::getDeleted, false));
            if (!CollectionUtils.isEmpty(oldFks)) {
                List<SecurityTableModelForeignKey> newFks = oldFks.stream().map(fk -> {
                    SecurityTableModelForeignKey newFk = new SecurityTableModelForeignKey();
                    newFk.setTableId(newTableId);
                    newFk.setConstraintName(fk.getConstraintName());
                    newFk.setColumnName(fk.getColumnName());
                    newFk.setReferencedTableName(fk.getReferencedTableName());
                    newFk.setReferencedColumnName(fk.getReferencedColumnName());
                    newFk.setUpdateRule(fk.getUpdateRule());
                    newFk.setDeleteRule(fk.getDeleteRule());
                    newFk.setDataType(fk.getDataType());
                    newFk.setRemark(fk.getRemark());
                    return newFk;
                }).toList();
                securityTableModelForeignKeyService.saveBatch(newFks);
            }
        }

        // 为选中的 api_table_model 添加 config 记录
        Set<String> selectedApiIds = new HashSet<>(dto.apiIds());
        for (SecurityApiTableModel binding : relatedBindings) {
            if (selectedApiIds.contains(binding.getApiId())) {
                // 先删除旧的 config
                configMapper.delete(new LambdaQueryWrapper<SecurityApiTableModelConfig>()
                        .eq(SecurityApiTableModelConfig::getTableModelId, binding.getId()));
                // 新增 config
                SecurityApiTableModelConfig config = new SecurityApiTableModelConfig();
                config.setTableModelId(binding.getId());
                config.setDatasource(newDatasource);
                configMapper.insert(config);
            }
        }
    }

    return true;
}
```

注意：需要在类中注入 `SecurityApiTableModelConfigMapper configMapper` 和 `List<BusinessModuleInfoProvider> providers`。

- [ ] **Step 8: Commit**

```bash
git add business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/service/
git commit -m "feat: 实现表模型管理 Service 层方法"
```

---

## Task 3: 后端 - Controller 层新增接口

**Files:**
- Modify: `business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/controller/SecurityTableModelTableController.java`

- [ ] **Step 1: 新增6个接口**

在 SecurityTableModelTableController 中新增：

```java
@Operation(summary = "分页查询表模型列表")
@PostMapping("page")
public R<IPage<TableModelTableVO>> page(@RequestBody TableModelTableQueryDTO query) {
    return R.ok(securityTableModelTableService.pageByCondition(query));
}

@Operation(summary = "查询未采集的表模型列表")
@GetMapping("uncollected")
public R<List<TableModelTableVO>> listUncollected(@RequestParam String modulePrefix) {
    return R.ok(securityTableModelTableService.listUncollected(modulePrefix));
}

@Operation(summary = "采集表模型")
@PostMapping("collect")
public R<Boolean> collect(@RequestBody TableModelCollectDTO dto) {
    return R.ok(securityTableModelTableService.collectTableModels(dto));
}

@Operation(summary = "自定义添加表模型")
@PostMapping("custom-save")
public R<TableModelTableVO> customSave(@RequestBody TableModelCustomSaveDTO dto) {
    return R.ok(securityTableModelTableService.customSave(dto));
}

@Operation(summary = "同步表模型字段")
@PostMapping("sync/{id}")
public R<Boolean> sync(@PathVariable String id) {
    return R.ok(securityTableModelTableService.syncTableModel(id));
}

@Operation(summary = "修改数据源")
@PostMapping("change-datasource")
public R<Boolean> changeDatasource(@RequestBody TableModelChangeDatasourceDTO dto) {
    return R.ok(securityTableModelTableService.changeDatasource(dto));
}
```

- [ ] **Step 2: Commit**

```bash
git add business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/controller/SecurityTableModelTableController.java
git commit -m "feat: 新增表模型管理 Controller 接口"
```

---

## Task 4: 前端 - 类型定义与 API 服务

**Files:**
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/types/index.ts`
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/services/tableModel.ts`
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/permissionConstants.ts`

- [ ] **Step 1: 创建类型定义 types/index.ts**

```typescript
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
```

- [ ] **Step 2: 创建 API 服务 services/tableModel.ts**

```typescript
import { get, post } from '@gwsu/core';
import type {
  TableModelQuery,
  TableModelPageResult,
  TableModelDetail,
  TableModelInfo,
  CollectItem,
  ChangeDatasourceRequest,
  ApiResourceSimple,
} from '../types';

const BASE = '/security/tablemodel';

/** 分页查询表模型列表 */
export async function getTableModelPage(query: TableModelQuery) {
  const res = await post<TableModelPageResult>(`${BASE}/page`, query);
  return res.data;
}

/** 查询表模型详情 */
export async function getTableModelDetail(
  modulePrefix: string,
  datasource: string,
  tableName: string,
): Promise<TableModelDetail> {
  const res = await post<TableModelDetail>(`${BASE}/detail`, {
    modulePrefix,
    datasource,
    tableName,
  });
  return res.data;
}

/** 查询未采集的表模型列表 */
export async function getUncollectedList(
  modulePrefix: string,
): Promise<TableModelInfo[]> {
  const res = await get<TableModelInfo[]>(`${BASE}/uncollected`, {
    modulePrefix,
  });
  return res.data ?? [];
}

/** 采集表模型 */
export async function collectTableModels(
  items: CollectItem[],
): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/collect`, { items });
  return res.data;
}

/** 自定义添加表模型 */
export async function customSaveTableModel(data: {
  applicationName: string;
  modulePrefix: string;
  datasource: string;
  tableName: string;
}): Promise<TableModelInfo> {
  const res = await post<TableModelInfo>(`${BASE}/custom-save`, data);
  return res.data;
}

/** 同步表模型字段 */
export async function syncTableModel(id: string): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/sync/${id}`);
  return res.data;
}

/** 修改数据源 */
export async function changeDatasource(
  data: ChangeDatasourceRequest,
): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/change-datasource`, data);
  return res.data;
}

/** 修改字段注释 */
export async function updateColumn(
  data: Record<string, unknown>,
): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/column`, data);
  return res.data;
}

/** 修改外键 */
export async function updateForeignKey(
  data: Record<string, unknown>,
): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/foreign-key`, data);
  return res.data;
}

/** 获取指定表模型关联的接口资源 */
export async function getApiResourcesByTableModel(data: {
  modulePrefix: string;
  datasource: string;
  tableName: string;
}): Promise<ApiResourceSimple[]> {
  const res = await post<ApiResourceSimple[]>(
    '/security/apiResource/listByTableModel',
    data,
  );
  return res.data ?? [];
}

/** 通过服务名获取数据源列表 */
export async function getDatasourceList(
  serverName: string,
): Promise<string[]> {
  const res = await get<string[]>('/security/apiResource/getDatasourceList', {
    serverName,
  });
  return res.data ?? [];
}

/** 获取模块列表 */
export async function getModuleList() {
  const res = await post<
    { prefix: string; applicationName: string; note: string }[]
  >('/modules/list');
  return res.data ?? [];
}
```

- [ ] **Step 3: 创建权限常量 permissionConstants.ts**

```typescript
/** 表模型管理 - 按钮权限标识常量 */

/** 表格头部操作权限 */
export const PERM_COLLECT = 'tablemodel_collect';
export const PERM_CUSTOM_ADD = 'tablemodel_custom_add';

/** 操作列下拉菜单权限 */
export const PERM_EDIT = 'tablemodel_edit';
export const PERM_SYNC = 'tablemodel_sync';
export const PERM_CHANGE_DATASOURCE = 'tablemodel_change_datasource';
```

- [ ] **Step 4: Commit**

```bash
git add web/apps/gwsu-sub-security/src/pages/tablemodel/
git commit -m "feat: 前端表模型页面 - 类型定义与API服务"
```

---

## Task 5: 前端 - useTableModel Hook + 主列表页

**Files:**
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/hooks/useTableModel.ts`
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/index.tsx`
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/index.module.less`

- [ ] **Step 1: 创建 useTableModel Hook**

```typescript
import { useState, useCallback, useRef } from 'react';
import { App } from 'antd';
import {
  getTableModelPage,
  syncTableModel,
  collectTableModels,
  customSaveTableModel,
  changeDatasource,
} from '../services/tableModel';
import type { TableModelInfo, TableModelQuery, CollectItem } from '../types';

export function useTableModel() {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<TableModelInfo[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const queryRef = useRef<TableModelQuery>({ pageNum: 1, pageSize: 10 });
  const initializedRef = useRef(false);

  const fetchTableModelPage = useCallback(
    async (query?: TableModelQuery) => {
      if (query) queryRef.current = query;
      setLoading(true);
      try {
        const params: TableModelQuery = {
          ...queryRef.current,
          pageNum: query?.pageNum ?? currentPage,
          pageSize: query?.pageSize ?? pageSize,
        };
        const page = await getTableModelPage(params);
        setDataSource(page?.records ?? []);
        setTotal(page?.total ?? 0);
        setCurrentPage(page?.current ?? 1);
        setPageSize(page?.size ?? 10);
      } catch {
        // request 层已自动提示
      } finally {
        setLoading(false);
      }
    },
    [currentPage, pageSize],
  );

  const ensureInitialized = useCallback(() => {
    if (!initializedRef.current) {
      initializedRef.current = true;
      fetchTableModelPage();
    }
  }, [fetchTableModelPage]);

  const handlePageChange = useCallback(
    (page: number, size: number) => {
      fetchTableModelPage({
        ...queryRef.current,
        pageNum: page,
        pageSize: size,
      });
    },
    [fetchTableModelPage],
  );

  const handleSync = useCallback(
    async (id: string) => {
      try {
        await syncTableModel(id);
        message.success('同步成功');
        await fetchTableModelPage();
        return true;
      } catch {
        return false;
      }
    },
    [fetchTableModelPage, message],
  );

  const handleCollect = useCallback(
    async (items: CollectItem[]) => {
      try {
        await collectTableModels(items);
        message.success('采集成功');
        await fetchTableModelPage();
        return true;
      } catch {
        return false;
      }
    },
    [fetchTableModelPage, message],
  );

  const handleCustomSave = useCallback(
    async (data: {
      applicationName: string;
      modulePrefix: string;
      datasource: string;
      tableName: string;
    }) => {
      try {
        await customSaveTableModel(data);
        message.success('添加成功');
        await fetchTableModelPage();
        return true;
      } catch {
        return false;
      }
    },
    [fetchTableModelPage, message],
  );

  const handleChangeDatasource = useCallback(
    async (data: {
      tableModelId: string;
      newDatasource: string;
      apiIds?: string[];
    }) => {
      try {
        await changeDatasource(data);
        message.success('修改数据源成功');
        await fetchTableModelPage();
        return true;
      } catch {
        return false;
      }
    },
    [fetchTableModelPage, message],
  );

  return {
    loading,
    dataSource,
    total,
    currentPage,
    pageSize,
    fetchTableModelPage,
    ensureInitialized,
    handlePageChange,
    handleSync,
    handleCollect,
    handleCustomSave,
    handleChangeDatasource,
  };
}
```

- [ ] **Step 2: 创建主列表页 index.tsx**

核心结构：搜索栏（所属服务、表名、来源类型） + 表格 + 操作按钮（采集、自定义添加、同步、修改数据源） + 详情。

搜索栏使用 Form，表格列：序号、表名、所属服务、数据源、表注释、来源类型、操作。
操作列：详情 + 更多下拉（同步、修改数据源、编辑字段、编辑外键）。
注意：操作列中**没有删除**。

详情使用抽屉展示字段表格和外键表格。

编辑字段/外键使用内联编辑模式（在详情抽屉中直接点击编辑按钮弹出表单）。

组件较多，主页面负责状态管理和组件组合，子组件通过 props 回调。

- [ ] **Step 3: 创建主列表页样式 index.module.less**

- [ ] **Step 4: Commit**

```bash
git add web/apps/gwsu-sub-security/src/pages/tablemodel/
git commit -m "feat: 前端表模型主列表页"
```

---

## Task 6: 前端 - 采集弹窗组件（步骤条）

**Files:**
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/components/CollectModal/index.tsx`
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/components/CollectModal/index.module.less`

- [ ] **Step 1: 实现采集弹窗**

核心逻辑：
1. 步骤条：2 步（①选择服务并解析 → ②确认采集）
2. Step 1：选择所属服务（下拉框），点击"解析"按钮，调用 `getUncollectedList` 获取未采集列表
3. 展示未采集的表模型列表（Table 展示，**没有 checkbox**），解析过程中显示 loading
4. Step 2：点击"下一步"直接进入确认步骤，展示将要采集的表模型
5. 点击"确定采集"调用 `collectTableModels` 接口
6. 采集过程中按钮 loading

Props:
```typescript
interface CollectModalProps {
  visible: boolean;
  onClose: () => void;
  onSuccess: () => void;
}
```

- [ ] **Step 2: Commit**

---

## Task 7: 前端 - 自定义添加弹窗

**Files:**
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/components/CustomAddModal/index.tsx`
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/components/CustomAddModal/index.module.less`

- [ ] **Step 1: 实现自定义添加弹窗**

核心逻辑：
1. 表单：所属服务（下拉，联动数据源）、数据源（下拉，联动表名）、表名（下拉，从库中获取）
2. 选择所属服务后，通过 `getDatasourceList` 获取数据源列表
3. 选择数据源后，通过 `/security/tablemodel/table/info` 获取表名列表
4. 选择表名后，输入表注释（可选）
5. 提交调用 `customSaveTableModel`
6. 唯一性校验：提交前前端提示，后端也会校验

Props:
```typescript
interface CustomAddModalProps {
  visible: boolean;
  onClose: () => void;
  onSuccess: () => void;
}
```

- [ ] **Step 2: Commit**

---

## Task 8: 前端 - 详情抽屉（含字段+外键编辑）

**Files:**
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/components/DetailDrawer/index.tsx`
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/components/DetailDrawer/index.module.less`
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/components/ColumnEditModal/index.tsx`
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/components/ColumnEditModal/index.module.less`
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/components/ForeignKeyEditModal/index.tsx`
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/components/ForeignKeyEditModal/index.module.less`

- [ ] **Step 1: 实现详情抽屉**

核心逻辑：
1. 展示表模型基本信息（表名、所属服务、数据源、来源类型、表注释）
2. Tabs 切换：字段列表 / 外键列表
3. 字段列表 Table：字段名、类型、长度、精度、可空、主键、默认值、注释、操作
4. 外键列表 Table：约束名、字段名、引用表名、引用字段名、数据类型、备注、操作
5. 字段操作：
   - 采集类型(sourceType=0)：只能修改注释（columnComment），点击编辑弹出 ColumnEditModal，只显示注释输入框
   - 自定义类型(sourceType=1)：只能修改注释（columnComment），同上
6. 外键操作：
   - 采集且 dataType=0：禁止修改关键内容，只可修改备注（remark）
   - 自定义添加(dataType=1)：可以任意修改
   - 点击编辑弹出 ForeignKeyEditModal

- [ ] **Step 2: 实现 ColumnEditModal**

- [ ] **Step 3: 实现 ForeignKeyEditModal**

- [ ] **Step 4: Commit**

---

## Task 9: 前端 - 修改数据源弹窗

**Files:**
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/components/ChangeDatasourceModal/index.tsx`
- Create: `web/apps/gwsu-sub-security/src/pages/tablemodel/components/ChangeDatasourceModal/index.module.less`

- [ ] **Step 1: 实现修改数据源弹窗**

核心逻辑：
1. 显示当前表模型信息（表名、所属服务、当前数据源）
2. 新数据源下拉框（通过 `getDatasourceList` 获取）
3. 关联接口选择：
   - 调用 `getApiResourcesByTableModel` 获取关联的接口列表
   - 使用 Transfer 或 Checkbox.Group 展示
   - 默认全选（如果不手动取消，等于修改所有接口）
   - 用户说明：不选择任何特定接口 = 修改所有接口的数据源
4. 提示信息：
   - 如果选择了部分接口：提示"将仅修改选中接口的数据源，会创建新的表模型数据"
   - 如果未选择（全选）：提示"将修改所有关联接口的数据源"
5. 提交调用 `changeDatasource`

Props:
```typescript
interface ChangeDatasourceModalProps {
  visible: boolean;
  data: TableModelInfo | null;
  onClose: () => void;
  onSuccess: () => void;
}
```

- [ ] **Step 2: Commit**

---

## Task 10: 前端 - 添加路由

**Files:**
- Modify: `web/apps/gwsu-sub-security/config/routes.ts`

- [ ] **Step 1: 添加 /tablemodel 路由**

在 routes.ts 中新增：
```typescript
{
  path: '/tablemodel',
  component: '@/pages/tablemodel',
},
```

注意：**主应用路由不需要添加**（用户明确要求）。

- [ ] **Step 2: Commit**

---

## Task 11: 后端构建验证

- [ ] **Step 1: 构建后端项目验证编译**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic && mvn clean install -DskipTests -pl business/business-security/business-security-server -am
```

确保无编译错误。

---

## Task 12: 前端构建验证

- [ ] **Step 1: 构建前端验证**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm build:sub-security
```

确保无编译错误。

---

## 自查清单

### 1. 需求覆盖

| 需求 | 任务 |
|------|------|
| 采集类型表模型 | Task 2 (listUncollected + collectTableModels), Task 6 (采集弹窗) |
| 自定义添加表模型 | Task 2 (customSave), Task 7 (自定义添加弹窗) |
| 修改数据源（采集类型） | Task 2 (changeDatasource), Task 9 (修改数据源弹窗) |
| config 表先删旧数据再新增 | Task 2 Step 7 中已处理 |
| 字段只能修改注释 | Task 8 (DetailDrawer + ColumnEditModal) |
| 采集外键禁止修改关键内容 | Task 8 (ForeignKeyEditModal) |
| 自定义外键可任意修改 | Task 8 (ForeignKeyEditModal) |
| 同步字段（增/删，不改已有） | Task 2 (syncTableModel) |
| 列表操作中没有删除 | Task 5 (主列表页操作列设计) |
| 采集弹窗无 checkbox + 步骤条 | Task 6 (CollectModal) |
| 主应用路由不添加 | Task 10 只修改子应用路由 |
| 唯一性判断（服务+数据源+表名） | Task 2 (customSave + changeDatasource) |
| 获取关联接口 | Task 9 (getApiResourcesByTableModel) |

### 2. 占位符扫描

无 TBD/TODO 等占位符。

### 3. 类型一致性

- 后端 DTO/VO 字段名与数据库列名一致（MyBatis Plus 自动映射）
- 前端 TypeScript 接口字段名与后端 VO JSON 字段名一致（camelCase）
- API 路径前缀统一使用 `/security/tablemodel`
