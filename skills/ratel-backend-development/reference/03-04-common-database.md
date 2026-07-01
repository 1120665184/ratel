# common-database — 数据库与多数据源

## DatabaseHelper — 数据库信息（Spring Bean 注入）

`getCurrentDatabaseType()` → 当前数据库类型枚举（`DatabaseType.POSTGRESQL` / `DatabaseType.MYSQL`）

## 多数据源

```java
@DS("master")  public void queryMasterDb() { ... }
@DS("mysql")   public void queryMysqlDb() { ... }
```

## 动态数据库标识

`DynamicDatabaseIdProvider` 根据当前数据源类型自动选择对应 SQL 语句（`databaseId="postgresql"` 或 `"mysql"`），Mapper XML 中可用：

```xml
<select id="selectPageVo" databaseId="postgresql">...</select>
<select id="selectPageVo" databaseId="mysql">...</select>
```

## 审计字段自动填充

`DefaultMetaObjectHandler` 自动填充 createOp/createTime/modifyOp/modifyTime，继承 `BaseDO` 自动生效。

## 雪花 ID

`DefaultIdentifierGenerator` 配合 `@TableId(type = IdType.ASSIGN_ID)` 自动生成。

## SqlExecutor — 通用 SQL 执行器

直接执行 SQL 并返回结构化结果（限 1000 行，超时 30s）：

```java
String[][] result = SqlExecutor.executeSqlAndReturnArr(connection, sql);
```

## ResultSetConverter — 结果集转换

将 JDBC `ResultSet` 转为 `List<String[]>` 结构化数据。

## 数据库元数据

| 类 | 说明 |
|----|------|
| `DdlFactory` | DDL 语句工厂，按数据库类型生成 |
| `MysqlMetadataDialect` | MySQL 元数据方言 |
| `PostgresqlMetadataDialect` | PostgreSQL 元数据方言 |
| `DatabaseInfo/TableInfo/ColumnInfo/ForeignKeyInfo` | 元数据模型 |

## BooleanTypeHandler — 布尔类型处理器

将数据库 SMALLINT/INT2 与 Java Boolean 互转（配合 SQL 规范中的布尔字段约定）。
