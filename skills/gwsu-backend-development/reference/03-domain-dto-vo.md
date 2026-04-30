# 三、Domain / DTO / VO 对象规范

## 3.1 Domain 对象规范

### 基本要求

- 继承 `org.quyq.gwsu.common.core.domain.BaseDO`
- 使用 `@TableName` 注解指定表名
- 主键使用 `@TableId(type = IdType.ASSIGN_ID)`
- 必须有 `toVo()` 方法转换为 VO 对象
- 必须有 `toDo()` 静态方法将 VO 转换为 DO 对象（减少 DTO Bean 的创建，saveOrUpdate 入参直接使用 VO）
- 类和属性均需有 `@Schema` 注解

### 完整示例

```java
package org.quyq.gwsu.xxx.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.xxx.api.vo.XxxVO;

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

    @Schema(description = "状态：0-禁用 1-正常")
    private Integer status;

    /**
     * 转换为 VO 对象
     */
    public XxxVO toVo() {
        XxxVO vo = new XxxVO();
        vo.setId(this.id);
        vo.setFieldName(this.fieldName);
        vo.setStatus(this.status);
        vo.copyBaseProperties(this);  // 复制基础属性
        return vo;
    }

    /**
     * VO 转 DO 对象
     */
    public static XxxEntity toDo(XxxVO vo) {
        XxxEntity entity = new XxxEntity();
        entity.setId(vo.getId());
        entity.setFieldName(vo.getFieldName());
        entity.setStatus(vo.getStatus());
        return entity;
    }
}
```

### BaseDO 包含的字段

| 字段         | 类型            | 说明         |
|------------|---------------|------------|
| tenantId   | String        | 租户ID       |
| modifyOp   | String        | 修改人        |
| modifyTime | LocalDateTime | 修改时间       |
| createOp   | String        | 创建人        |
| createTime | LocalDateTime | 创建时间       |
| deleted    | Boolean       | 删除标识（逻辑删除） |
| deleteOp   | String        | 删除人        |
| deleteTime | LocalDateTime | 删除时间       |

## 3.2 DTO 对象规范

### 基本要求

- 类名必须以 `DTO` 为后缀
- 继承 `org.quyq.gwsu.common.core.domain.BaseDTO`
- 统一在 **api 模块** 中定义
- 用于接收前端请求参数

### 完整示例

```java
package org.quyq.gwsu.xxx.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "XXX查询条件")
public class XxxQueryDTO extends BaseDTO {

    @Schema(description = "名称（模糊查询）")
    private String name;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
```

### BaseDTO 包含的字段

| 字段            | 类型      | 默认值   | 说明          |
|---------------|---------|-------|-------------|
| pageNum       | Integer | 1     | 页码          |
| pageSize      | Integer | 10    | 每页记录数       |
| pageFrom      | Integer | 0     | 页码偏移量（自动计算） |
| orderByColumn | String  | -     | 排序列         |
| asc           | String  | "asc" | 排序方向        |

## 3.3 VO 对象规范

### 基本要求

- 必须继承 `org.quyq.gwsu.common.core.domain.BaseVO`
- 类名必须以 `VO` 为后缀，全大写
- 统一在 **api 模块** 中定义
- 用于返回给前端的响应数据
- 类和属性均需有 `@Schema` 注解

### 完整示例

```java
package org.quyq.gwsu.xxx.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "XXX信息")
public class XxxVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "字段名称")
    private String fieldName;

    @Schema(description = "状态：0-禁用 1-正常")
    private Boolean status;
}
```
