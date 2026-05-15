# 四、Mapper / Service / Controller 分层规范

## 4.1 Mapper 对象规范

### 基本要求

- 必须继承 `BaseMapper<T>` 接口
- 自定义查询方法命名符合 Spring Data JPA 风格
- 分页方法统一返回 `IPage<T>`
- XML 文件放在 `resources/mapper/{业务名}/` 目录下

### 接口示例

```java
package org.quyq.gwsu.xxx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.xxx.domain.XxxEntity;
import org.quyq.gwsu.xxx.api.vo.XxxVO;

public interface XxxEntityMapper extends BaseMapper<XxxEntity> {

    /**
     * 分页查询
     */
    IPage<XxxVO> selectPageVo(Page<XxxVo> page, @Param("query") XxxQueryDTO query);

    /**
     * 根据名称查询列表
     */
    List<Xxx> selectByName(@Param("name") String name);
}
```

### XML 示例

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.quyq.gwsu.xxx.mapper.XxxEntityMapper">

    <select id="selectPageVo" resultType="org.quyq.gwsu.xxx.api.vo.XxxVO">
        SELECT id, field_name, status, create_time
        FROM xxx_entity
        WHERE deleted = 0
        <if test="query.name != null and query.name != ''">
            AND field_name LIKE CONCAT('%', #{query.name}, '%')
        </if>
        <if test="query.status != null">
            AND status = #{query.status}
        </if>
        <if test="query.orderBy != null and query.orderBy != ''">
            ORDER BY ${query.orderBy}
        </if>
    </select>

</mapper>
```

## 4.2 Service 层规范

### 接口规范

- 接口名以 `I` 开头，以 `Service` 结尾
- 继承 `IService<T>`
- 方法定义遵循命名规范

```java
package org.quyq.gwsu.xxx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.xxx.domain.XxxEntity;
import org.quyq.gwsu.xxx.api.vo.XxxVO;
import org.quyq.gwsu.xxx.api.dto.XxxQueryDTO;

import java.util.List;

public interface IXxxService extends IService<XxxEntity> {

    // 查询单条：getBy + 条件
    XxxVO getById(String id);

    XxxVO getByName(String name);

    // 查询列表：listBy + 条件
    List<XxxVO> listByStatus(Integer status);

    List<XxxVO> listByCondition(XxxQueryDTO query);

    // 分页查询
    IPage<XxxVO> pageByCondition(XxxQueryDTO query);

    // 保存：save + 实体描述
    Boolean saveEntity(XxxEntity entity);

    // 更新：update + 实体描述
    Boolean updateEntity(XxxEntity entity);

    // 删除：remove + 条件
    Boolean removeByIds(List<String> ids);

    // 导出：export + 条件
    void exportData(XxxQueryDTO query);

    // 导入：import + 数据
    void importData(List<XxxEntity> dataList);
}
```

### 实现类规范

- 实现类名以 `ServiceImpl` 结尾
- 继承 `ServiceImpl<Mapper, Entity>` 和业务接口
- 无需以 `I` 开头

```java
package org.quyq.gwsu.xxx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.xxx.api.dto.XxxQueryDTO;
import org.quyq.gwsu.xxx.api.vo.XxxVO;
import org.quyq.gwsu.xxx.domain.XxxEntity;
import org.quyq.gwsu.xxx.mapper.XxxEntityMapper;
import org.quyq.gwsu.xxx.service.IXxxService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class XxxServiceImpl extends ServiceImpl<XxxEntityMapper, XxxEntity> implements IXxxService {

    @Override
    public XxxVO getById(String id) {
        XxxEntity entity = super.getById(id);
        return entity != null ? entity.toVo() : null;
    }

    @Override
    public XxxVO getByName(String name) {
        XxxEntity entity = getOne(new LambdaQueryWrapper<XxxEntity>()
                .eq(XxxEntity::getFieldName, name));
        return entity != null ? entity.toVo() : null;
    }

    @Override
    public List<XxxVO> listByStatus(Integer status) {
        return list(new LambdaQueryWrapper<XxxEntity>()
                .eq(XxxEntity::getStatus, status))
                .stream()
                .map(XxxEntity::toVo)
                .toList();
    }

    @Override
    public IPage<XxxVO> pageByCondition(XxxQueryDTO query) {
        Page<XxxVo> page = new Page<>(query.getPageNum(), query.getPageSize());
        return baseMapper.selectPageVo(page, query);
    }

    @Override
    public Boolean saveEntity(XxxEntity entity) {
        return save(entity);
    }

    @Override
    public Boolean updateEntity(XxxEntity entity) {
        return updateById(entity);
    }

    @Override
    public Boolean removeByIds(List<String> ids) {
        return removeBatchByIds(ids);
    }
}
```

### 方法命名规范

| 操作类型 | 命名规则        | 示例                                         |
|------|-------------|--------------------------------------------|
| 单条查询 | getBy + 条件  | `getById`, `getByName`                     |
| 列表查询 | listBy + 条件 | `listByStatus`, `listByCondition`          |
| 分页查询 | pageBy + 条件 | `pageByCondition`                          |
| 保存   | save + 描述   | `saveEntity`, `saveBatch`                  |
| 更新   | update + 描述 | `updateEntity`, `updateStatus`             |
| 删除   | remove + 条件 | `removeById`, `removeByIds`, `removeBatch` |
| 导出   | export + 描述 | `exportData`                               |
| 导入   | import + 描述 | `importData`                               |

## 4.3 Controller 层规范

### 基本要求

- 查询类接口参数为对象时使用 DTO 对象，新增/编辑类接口入参使用 VO 对象（减少 DTO Bean 的创建）
- 统一返回 `R<T>` 类型
- 返回业务对象时必须是 VO 对象
- 类头加 `@Tag`，方法加 `@Operation` 注解
- **类头必须加 `@TableModelPermission` 注解**，声明该 Controller 操作涉及的表模型
- 遵循 RESTful 协议
- 保存和更新合并为 `saveOrUpdate` 方法
- 新增方法中必填字段必须使用 `AssertUtils` 进行验证，验证方式为 `AssertUtils.hasText(dto.getField(), ErrorCode)` 等，错误码需在对应模块的错误码枚举中定义

### @TableModelPermission — 表模型权限注解

> 包路径：`org.quyq.gwsu.common.security.annotation.TableModelPermission`

**所有 Controller 类必须在类上添加 `@TableModelPermission` 注解**，声明该 Controller 操作涉及的数据库表模型。此注解由 `ApiEndpointCollector` 在应用启动时自动采集，用于表模型级别的权限控制。

#### 注解属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `value` | `Class<?>[]` | 表模型对应的 Domain 类列表（类需有 `@TableName` 注解） |
| `tables` | `String[]` | 表名列表（直接指定表名，与 `value` 互为补充） |

#### 生效规则

| 场景 | 行为 |
|------|------|
| 无注解 | 无表模型权限 |
| 仅类上 | 该 Controller 所有方法继承类上的表模型权限 |
| 仅方法上 | 使用方法上的表模型权限 |
| 类+方法（方法有配置） | 方法上的配置覆盖类上的配置 |
| 类+方法（空注解 `@TableModelPermission`） | 该接口不继承类上的表模型权限 |

#### 使用方式

**方式一：通过 Domain 类引用（推荐）**，自动从 `@TableName` 注解获取表名：

```java
@TableModelPermission({SecurityRole.class, SecurityRoleMenu.class})
public class SecurityRoleController { ... }
```

**方式二：直接指定表名**，适用于没有 Domain 类或跨模块表：

```java
@TableModelPermission(tables = {"security_role", "security_role_menu"})
public class SecurityRoleController { ... }
```

**方式三：混合使用**：

```java
@TableModelPermission(value = {SecurityRole.class}, tables = {"other_table"})
public class SecurityRoleController { ... }
```

#### 方法级覆盖示例

```java
@TableModelPermission({SecurityRole.class, SecurityRoleMenu.class})
public class SecurityRoleController {

    // 继承类上的表模型权限

    @GetMapping("/public-info")
    @TableModelPermission   // 空注解：该接口不继承类上的表模型权限
    public R<PublicInfoVO> getPublicInfo() { ... }

    @GetMapping("/detail")
    @TableModelPermission(SecurityRole.class)  // 仅使用 SecurityRole 表
    public R<RoleVO> getDetail() { ... }
}
```

#### 新增表模型时的规则

当新增一个 Domain 类（对应新的数据库表）时，需要在其所属业务模块的 **Controller** 类上的 `@TableModelPermission` 注解中补充该 Domain 类：

1. 确定新 Domain 类归属的业务模块
2. 找到该模块对应的 Controller
3. 在 `@TableModelPermission` 的 `value` 数组中添加新的 Domain 类

示例：假设 `SecurityRoleSubject` 是新增的 Domain 类，需要更新 Controller：

```java
// 更新前
@TableModelPermission({SecurityRole.class, SecurityRoleMenu.class})
public class SecurityRoleController { ... }

// 更新后：补充 SecurityRoleSubject.class
@TableModelPermission({SecurityRole.class, SecurityRoleMenu.class, SecurityRoleSubject.class})
public class SecurityRoleController { ... }
```

### @TableModelField — 表模型字段配置注解

> 包路径：`org.quyq.gwsu.common.security.annotation.TableModelField`

标注在 Domain 类字段上，配置该字段在表模型权限中的行为。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `show` | `boolean` | `true` | 是否允许 AI 查询该字段 |
| `desensitize` | `boolean` | `false` | 返回给用户时是否脱敏 |
| `strategy` | `SensitiveStrategy` | `NONE` | 脱敏策略 |
| `prefixNoMaskLen` | `int` | `1` | 自定义脱敏-不脱敏前缀长度（仅 `strategy=CUSTOM`） |
| `suffixNoMaskLen` | `int` | `1` | 自定义脱敏-不脱敏后缀长度（仅 `strategy=CUSTOM`） |
| `symbol` | `String` | `"*"` | 自定义脱敏-脱敏标识符（仅 `strategy=CUSTOM`） |

```java
@TableName("security_user")
public class SecurityUser extends BaseDO {

    @TableModelField
    private String username;

    @TableModelField(show = false)          // AI 不可查询
    private String password;

    @TableModelField(desensitize = true, strategy = SensitiveStrategy.PHONE)  // 手机号脱敏
    private String phone;

    @TableModelField(desensitize = true, strategy = SensitiveStrategy.CUSTOM, prefixNoMaskLen = 2, suffixNoMaskLen = 1, symbol = "#")
    private String idCard;
}
```

### 完整示例

```java
package org.quyq.gwsu.xxx.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.xxx.api.dto.XxxQueryDTO;
import org.quyq.gwsu.xxx.api.vo.XxxVO;
import org.quyq.gwsu.xxx.domain.XxxEntity;
import org.quyq.gwsu.xxx.errcode.XxxErrorCode;
import org.quyq.gwsu.xxx.service.IXxxService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("xxx")
@Tag(name = "XXX管理", description = "XXX模块接口")
@RequiredArgsConstructor
@TableModelPermission({XxxEntity.class})
public class XxxController {

    private final IXxxService xxxService;

    @Operation(summary = "分页查询")
    @PostMapping("/page")
    public R<IPage<XxxVO>> page(@RequestBody XxxQueryDTO query) {
        return R.ok(xxxService.pageByCondition(query));
    }

    @Operation(summary = "根据ID查询")
    @GetMapping("/{id}")
    public R<XxxVO> getById(@PathVariable String id) {
        return R.ok(xxxService.getById(id));
    }

    @Operation(summary = "查询列表")
    @GetMapping("/list")
    public R<List<XxxVO>> list(@RequestParam(required = false) Integer status) {
        return R.ok(xxxService.listByStatus(status));
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

### RESTful 路由规范

| HTTP 方法 | 路径        | 操作           | 说明               |
|---------|-----------|--------------|------------------|
| GET     | /xxx      | list         | 查询列表             |
| GET     | /xxx/{id} | getById      | 查询单条             |
| POST    | /xxx/page | page         | 分页查询             |
| POST    | /xxx      | saveOrUpdate | 新增或更新            |
| DELETE  | /xxx      | remove       | 批量删除             |
| DELETE  | /xxx/{id} | removeById   | 单条删除             |
