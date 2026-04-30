# 五、API Client 规范

## 5.1 基本规则

- 新的功能模块新增时无需添加 API Client
- 不同的业务模块功能调用时需要配置 API Client，例如：business-system 业务模块的 user 功能模块调用 business-security 业务模块的 role 功能模块，需要 role 功能模块开发对应的 API Client。

## 5.2 API 接口定义

在 **api 模块** 中定义 `@ApiClient` 接口：

```java
package org.quyq.gwsu.xxx.api;

import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.xxx.api.fallback.XxxClientApiFallbackFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.HttpExchange;

@ApiClient(value = "gwsu-xxx", note = "XXX模块API", fallbackFactory = XxxClientApiFallbackFactory.class)
@HttpExchange("/xxx")
public interface XxxClientApi {

    @GetExchange("/{id}")
    R<XxxVO> getById(@PathVariable("id") String id);

    @PostExchange
    R<List<XxxVO>> listByCondition(@RequestBody XxxDTO entity);
}
```

## 5.3 Controller 实现 API 接口

**重要**：Controller 必须实现对应的 `XxxClientApi` 接口，以确保接口定义与实现一致：

```java
package org.quyq.gwsu.xxx.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.xxx.api.XxxClientApi;
import org.quyq.gwsu.xxx.api.dto.XxxQueryDTO;
import org.quyq.gwsu.xxx.api.vo.XxxVo;
import org.quyq.gwsu.xxx.domain.XxxEntity;
import org.quyq.gwsu.xxx.service.IXxxService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("xxx")
@Tag(name = "XXX管理", description = "XXX模块接口")
@RequiredArgsConstructor
public class XxxController implements XxxClientApi {

    private final IXxxService xxxService;

    @Operation(summary = "根据ID查询")
    @GetMapping("/{id}")
    @Override
    public R<XxxVo> getById(@PathVariable Long id) {
        return R.ok(xxxService.getById(id));
    }

    @Operation(summary = "通过条件查询")
    @PostMapping
    @Override
    public R<List<XxxVO>> listByCondition(@RequestBody XxxDTO entity) {
        return R.ok(xxxService.listByCondition(entity));
    }

    // 其他非 API 接口方法...
}
```

**说明**：

- Controller 实现 `XxxClientApi` 接口后，接口方法自动获得路由映射
- 需要在实现方法上添加 `@Override` 注解
- 仍需添加 `@Operation` 注解用于 Swagger 文档生成
- Controller 可以定义额外的非接口方法（如分页查询、批量删除等）

## 5.4 降级工厂实现

```java
package org.quyq.gwsu.xxx.api.fallback;

import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.xxx.api.XxxClientApi;
import org.springframework.stereotype.Component;

@Component
public class XxxClientApiFallbackFactory implements FallbackFactory<XxxClientApi> {

    @Override
    public XxxClientApi create(Throwable cause) {
        return new XxxClientApi() {
            @Override
            public R<XxxVO> getById(String id) {
                return R.fail("XXX服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public R<Boolean> saveOrUpdate(XxxEntity entity) {
                return R.fail("XXX服务暂时不可用: " + cause.getMessage());
            }
        };
    }
}
```
