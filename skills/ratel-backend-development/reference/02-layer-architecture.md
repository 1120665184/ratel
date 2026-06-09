# 二、分层架构规范

## 2.1 Domain 对象

- 继承 `BaseDO`（含 tenantId、审计字段、逻辑删除字段）
- `@TableName` + `@TableId(type = IdType.ASSIGN_ID)` + `@Schema`
- **必须有** `toVo()` 和 `static toDo(VO)` 方法
- `toVo()` 中使用 `vo.copyBaseProperties(this)` 复制基础属性

```java
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "xxx_entity", autoResultMap = true)
@Schema(description = "XXX实体表")
public class XxxEntity extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "字段名称")
    private String fieldName;

    public XxxVO toVo() {
        XxxVO vo = new XxxVO();
        vo.setId(this.id);
        vo.setFieldName(this.fieldName);
        vo.copyBaseProperties(this);
        return vo;
    }

    public static XxxEntity toDo(XxxVO vo) {
        XxxEntity entity = new XxxEntity();
        entity.setId(vo.getId());
        entity.setFieldName(vo.getFieldName());
        return entity;
    }
}
```

## 2.2 DTO 对象

- 继承 `BaseDTO`（含 pageNum=1、pageSize=10、orderByColumn、asc）
- 在 **api 模块** 中定义，用于接收前端请求参数

```java
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "XXX查询条件")
public class XxxQueryDTO extends BaseDTO {
    @Schema(description = "名称")
    private String name;
}
```

## 2.3 VO 对象

- 继承 `BaseVO`，在 **api 模块** 中定义，用于返回前端响应数据
- 类名以 `VO` 结尾（全大写），类和属性需 `@Schema` 注解

## 2.4 Mapper 层

- 继承 `BaseMapper<T>`
- 分页方法返回 `IPage<T>`
- XML 放在 `resources/mapper/{业务名}/` 目录

```java
public interface XxxEntityMapper extends BaseMapper<XxxEntity> {
    IPage<XxxVO> selectPageVo(Page<XxxVO> page, @Param("query") XxxQueryDTO query);
}
```

## 2.5 Service 层

- 接口：`I` + Domain + `Service`，继承 `IService<T>`
- 实现：Domain + `ServiceImpl`，继承 `ServiceImpl<Mapper, Entity>`

**方法命名**：

| 操作 | 命名 | 示例 |
|------|------|------|
| 单条查询 | getBy + 条件 | `getById`, `getByName` |
| 列表查询 | listBy + 条件 | `listByStatus` |
| 分页查询 | pageBy + 条件 | `pageByCondition` |
| 保存 | save + 描述 | `saveEntity` |
| 更新 | update + 描述 | `updateEntity` |
| 删除 | remove + 条件 | `removeByIds` |

## 2.6 Controller 层

- 查询入参用 DTO，新增/编辑入参用 VO
- 统一返回 `R<T>`，业务对象必须是 VO
- 类加 `@Tag`，方法加 `@Operation`
- **类头必须加 `@TableModelPermission`**
- 保存/更新合并为 `saveOrUpdate`
- 必填字段用 `AssertUtils` 验证

### @TableModelPermission — 表模型权限注解

所有 Controller 类必须添加，声明操作的数据库表模型：

```java
// 方式一：通过 Domain 类（推荐）
@TableModelPermission({SecurityRole.class, SecurityRoleMenu.class})

// 方法级覆盖
@TableModelPermission   // 空注解：不继承类上的权限
@TableModelPermission(SecurityRole.class)  // 覆盖类上的配置
```

### @TableModelField — 字段级权限

标注在 Domain 字段上：`@TableModelField(show=false)` AI 不可查询；`@TableModelField(desensitize=true, strategy=SensitiveStrategy.PHONE)` 脱敏

### RESTful 路由

| HTTP 方法 | 路径 | 操作 |
|---------|------|------|
| GET | /xxx | list |
| GET | /xxx/{id} | getById |
| POST | /xxx/page | page |
| POST | /xxx | saveOrUpdate |
| DELETE | /xxx | 批量删除 |
| DELETE | /xxx/{id} | 单条删除 |

### 完整示例

```java
@RestController
@RequestMapping("xxx")
@Tag(name = "XXX管理")
@RequiredArgsConstructor
@TableModelPermission({XxxEntity.class})
public class XxxController {
    private final IXxxService xxxService;

    @Operation(summary = "分页查询")
    @PostMapping("/page")
    public R<IPage<XxxVO>> page(@RequestBody XxxQueryDTO query) {
        return R.ok(xxxService.pageByCondition(query));
    }

    @Operation(summary = "新增或更新")
    @PostMapping
    public R<String> saveOrUpdate(@RequestBody XxxVO vo) {
        if (vo.getId() == null) {
            AssertUtils.hasText(vo.getFieldName(), XxxErrorCode.E00001);
        }
        return R.ok(xxxService.saveOrUpdateUser(vo));
    }

    @Operation(summary = "批量删除")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(xxxService.removeByIds(ids));
    }
}
```
