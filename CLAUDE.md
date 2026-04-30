# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此代码仓库中工作时提供指导。

## 构建命令

### 后端构建（Maven）

```bash
# 从项目根目录构建所有模块
mvn clean install

# 构建指定模块及其依赖
mvn clean install -pl business/application/distributed/gwsu-security -am

# 运行单体应用
mvn spring-boot:run -pl business/application/single/gwsu

# 运行分布式微服务（需要 Nacos 运行在 127.0.0.1:8848）
mvn spring-boot:run -pl business/application/distributed/gwsu-security

# 运行测试
mvn test -pl business/application/single/gwsu

# Native 镜像构建（需要 GraalVM）
mvn -Pnative package -pl business/application/single/gwsu

# 跳过测试构建
mvn clean install -DskipTests
```

### 前端构建（pnpm）

```bash
# 进入前端目录
cd web

# 安装依赖
pnpm install

# 开发模式
pnpm dev:main          # 仅启动主应用（端口 8000）
pnpm dev:sub-system    # 仅启动系统子应用（端口 8001）
pnpm dev:sub-security  # 仅启动安全子应用（端口 8002）
pnpm dev:all           # 并行启动所有应用

# 构建
pnpm build:core        # 先构建共享库 @gwsu/core
pnpm build:main        # 构建主应用
pnpm build:sub-system  # 构建系统子应用
pnpm build:sub-security # 构建安全子应用
pnpm build:all         # 构建所有

# 清理
pnpm clean             # 清理所有 node_modules、dist、.umi
```

## 项目架构

这是一个全栈项目，包含 **Java 后端**和**前端**两部分，支持双部署模式。

### 后端架构

基于 Spring Boot 4.0.3 / Java 25 的多模块 Maven 项目，支持**双部署模式**：单体应用或分布式微服务。

#### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 4.0.3 | 应用框架 |
| Spring Cloud | 2025.1.1 | 微服务框架 |
| Spring Cloud Alibaba | 2025.1.0.0 | Nacos 服务发现与配置 |
| MyBatis Plus | 3.5.16 | ORM 框架，支持多数据源 |
| Redisson | 4.3.0 | Redis 缓存客户端 |
| Resilience4j | 2.4.0 | 熔断器 |
| jCasbin | 1.99.0 | ABAC 权限控制 |
| Sa-Token | 1.45.0 | 认证框架 |
| Hutool | 5.8.44 | 工具库 |

### 前端架构

基于 UmiJS 4 + qiankun 的微前端应用，采用 pnpm monorepo 工作空间管理。

#### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| UmiJS | 4.x | React 企业级框架 |
| qiankun | - | 微前端框架 |
| Ant Design | 6.3.4 | UI 组件库 |
| ProComponents | 2.8.10 | 中后台组件库 |
| React | 18.x | 前端框架 |
| TypeScript | 5.x | 类型系统 |
| pnpm | - | 包管理器 |

#### 前端项目结构

```
web/
├── apps/                          # 应用目录
│   ├── gwsu-main/                 # 主应用（端口 8000）
│   │   ├── config/
│   │   │   ├── config.ts          # qiankun 主应用配置
│   │   │   └── routes.ts          # 路由配置
│   │   ├── src/
│   │   │   ├── layouts/           # 布局组件
│   │   │   ├── components/        # 业务组件
│   │   │   └── pages/             # 页面
│   │   └── package.json
│   ├── gwsu-sub-system/           # 子应用-系统管理（端口 8001）
│   │   ├── config/config.ts       # qiankun 子应用配置，base: /sub-system
│   │   └── src/
│   └── gwsu-sub-security/         # 子应用-安全中心（端口 8002）
│       ├── config/config.ts       # qiankun 子应用配置，base: /sub-security
│       └── src/
├── gwsu-core/                     # 共享核心库（@gwsu/core）
│   ├── src/
│   │   ├── components/            # 共享组件（ThemeLayout）
│   │   ├── constants/             # 常量（主题配置）
│   │   ├── types/                 # 类型定义
│   │   └── utils/                 # 工具函数
│   └── package.json
├── package.json                   # 工作空间根配置
└── pnpm-workspace.yaml           # pnpm 工作空间配置
```

#### 微前端架构模式

| 应用 | 角色 | 端口 | 说明 |
|------|------|------|------|
| gwsu-main | 主应用 | 8000 | ProLayout 外壳，通过 `<MicroApp>` 加载子应用 |
| gwsu-sub-system | 子应用 | 8001 | 系统管理模块，base 路径 `/sub-system` |
| gwsu-sub-security | 子应用 | 8002 | 安全中心模块，base 路径 `/sub-security` |

#### 主题系统

`@gwsu/core` 提供多主题系统：
- 6 种主题：ocean（默认）、forest、violet、amber、graphite、midnight（暗色模式）
- `ThemeLayout` 组件提供 `ThemeContext`，包含 `currentTheme` 和 `changeTheme`
- 通过 `window.postMessage({ type: 'THEME_CHANGE', payload: theme })` 跨应用同步主题
- 使用 CSS 变量和 Ant Design `ConfigProvider` 实现主题切换

### 模块结构

```
root-pom/                    # 根 POM，依赖和插件管理
project-pom/                 # GWSU 内部模块版本管理
common/                      # 公共基础设施模块
├── common-core              # 核心工具类、领域对象（R、BusinessModuleInfo）
├── common-api               # @ApiClient 框架，服务间调用
├── common-cache             # Redis/Redisson 配置
├── common-database          # MyBatis Plus、多数据源、动态数据库标识
├── common-deploy            # 部署模式配置（单体/分布式）
├── common-security          # Casbin/ABAC 安全配置
└── common-authentication    # 认证相关配置
business/                    # 业务领域模块
├── business-security/       # 安全模块（api + server）
│   ├── business-security-api    # API 接口和 DTO
│   └── business-security-server # 实现和控制器
├── business-test/           # 测试模块（api + server）
├── business-system/         # 系统模块（api + server）
└── application/
    ├── distributed/         # 微服务应用
    │   ├── gwsu-gateway     # API 网关
    │   ├── gwsu-security    # 安全服务
    │   ├── gwsu-test        # 测试服务
    │   └── gwsu-system      # 系统服务
    └── single/gwsu          # 单体应用，整合所有业务模块
```

### 部署模式

通过 `deploy.single` 属性控制，由 `DeployInitializer` 自动检测：

| 模式 | 属性值 | 说明 |
|------|--------|------|
| 单体模式 | `deploy.single: true` | 所有业务模块在一个应用中运行，API 调用使用 `LocalApiClientFactory` 直接查找 Bean |
| 分布式模式 | `deploy.single: false` | 每个模块是独立的微服务，注册到 Nacos，API 调用使用 `RemoteApiClientFactory` 进行 HTTP 调用并支持 Resilience4j 熔断 |

**自动检测逻辑**：检查是否存在 `NacosDiscoveryAutoConfiguration` 类，存在则为分布式模式。

### 业务模块开发模式

每个业务模块遵循统一的分层结构：

```
business-xxx/
├── business-xxx-api/        # API 模块
│   ├── XxxClientApi.java    # @ApiClient 接口
│   ├── XxxVo.java           # 值对象
│   └── XxxClientApiFallbackFactory.java  # 降级工厂
└── business-xxx-server/     # 服务模块
    ├── XxxModuleInfoProvider.java  # 模块信息提供者
    ├── controller/          # 控制器
    ├── service/             # 服务层
    ├── mapper/              # MyBatis Mapper
    └── domain/              # 领域对象
```

#### 创建新业务模块步骤

1. **实现 `BusinessModuleInfoProvider`**：注册模块前缀和描述
   ```java
   @Component
   public class XxxModuleInfoProvider implements BusinessModuleInfoProvider {
       @Override
       public BusinessModuleInfo module() {
           return new BusinessModuleInfo("xxx", "模块描述");
       }
   }
   ```

2. **创建 `@ApiClient` 接口**：定义服务间调用 API
   ```java
   @ApiClient(value = "gwsu-xxx", fallbackFactory = XxxClientApiFallbackFactory.class)
   public interface XxxClientApi {
       @GetExchange("/xxx/action")
       R<Data> action();
   }
   ```

3. **创建 `FallbackFactory`**：实现熔断降级
   ```java
   @Component
   public class XxxClientApiFallbackFactory implements FallbackFactory<XxxClientApi> {
       @Override
       public XxxClientApi create(Throwable cause) {
           return new XxxClientApi() {
               @Override
               public R<Data> action() {
                   return R.fail("服务暂时不可用: " + cause.getMessage());
               }
           };
       }
   }
   ```

4. **实现控制器**：实现 API 接口
   ```java
   @RestController
   public class XxxController implements XxxClientApi {
       @GetMapping("/xxx/action")
       @Override
       public R<Data> action() { ... }
   }
   ```

### API Client 框架

类似 `@FeignClient` 的服务间调用框架，支持双模式：

| 模式 | 工厂类 | 调用方式 |
|------|--------|----------|
| 单体 | `LocalApiClientFactory` | Spring Bean 直接查找 |
| 分布式 | `RemoteApiClientFactory` | HTTP 调用 + Resilience4j 熔断 |

**熔断配置优先级**：方法注解 > 类注解 > 配置文件

### 多数据源

使用 `@DS` 注解切换数据源：

```java
@DS("master")
public void queryMasterDb() { ... }

@DS("mysql")
public void queryMysqlDb() { ... }
```

支持动态数据库标识（DatabaseIdProvider），根据数据源类型自动选择对应的 SQL 语句。

### 配置管理

**单体应用**：从 classpath 导入配置
- `database.yaml` - 数据库配置
- `redis.yaml` - Redis 配置

**分布式应用**：从 Nacos 导入配置
- `common.yaml` - 公共配置
- `common-redis.yaml` - Redis 配置
- `common-database.yaml` - 数据库配置
- `{application.name}.yaml` - 应用专属配置

### 统一响应类型

所有 API 响应使用 `R<T>` record 类型：

```java
R.ok(data)              // 成功，带数据
R.ok(data, "msg")       // 成功，带数据和自定义消息
R.ok()                  // 成功，无数据
R.fail("msg")           // 失败，带消息
R.fail(exception)       // 失败，带 BasicException
```

### ABAC 权限控制

基于 jCasbin 实现的属性级访问控制：
- 权限策略存储在 Redis 中
- 支持字段级别的权限控制（`FieldEnforcer`）
- 自定义函数：`contains` 等

---

## 代码编写规范

### 后端规范

1. **禁止使用已过时的方法**
2. **所有代码必须是生产级别**，严谨、完整、可维护
3. **涉及动态代理、反射的实现必须考虑 AOT 兼容性**
   - 添加 `@ImportRuntimeHints` 注解
   - 提供 `RuntimeHintsRegistrar` 实现
   - 在 `META-INF/native-image/reachability-metadata.json` 中配置反射元数据

### 前端规范

1. **禁止使用已过时的方法**
2. **所有代码必须是企业级别**，遵循通用规范、逻辑严谨
3. **样式必须抽离成单独文件**：使用 `*.module.less` 方式，禁止 CSS-in-JS
4. **严格遵循组件化思想**：必要功能抽离成独立组件
5. **生成界面样式时参考相关技能**：使用 `/frontend-design` 等技能

#### 前端开发模式

**添加新子应用**：
1. 在 `apps/` 下创建应用，配置 `qiankun: { slave: {} }`
2. 添加到 `pnpm-workspace.yaml`
3. 在主应用 `config/config.ts` 的 `qiankun.master.apps` 中注册
4. 在主应用 `config/routes.ts` 添加路由，使用 `microApp: 'app-name'`
5. 子应用布局使用 `ThemeLayout` 包裹以同步主题

**从核心库导入**：
```tsx
import { ThemeLayout, useThemeContext, themes, getThemeByKey } from '@gwsu/core';
```

**修改主题**：
- 主题定义：`gwsu-core/src/constants/theme.ts`
- 类型定义：`gwsu-core/src/types/theme.ts`
- 工具函数：`gwsu-core/src/utils/theme.ts`
- 布局组件：`gwsu-core/src/components/ThemeLayout.tsx`

---

## 其他配置

1. **本地 Maven 仓库地址**：`/Users/quyq/Documents/work/m2/respository`
2. 全程都用**中文**

---

## 相关文档

- 后端详细说明：`CLAUDE.md`（本文件）
- 前端详细说明：`web/CLAUDE.md`
