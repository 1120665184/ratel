# OutputView 智能体设计文档

## 概述

实现智能助手将输出内容以精美可视化界面展示给用户的功能。后端 OutputViewAgent 子智能体输出 json-render spec，通过 AG-UI AGENT_OUTPUT 自定义事件流式传输到前端，前端使用 json-render Renderer 渐进渲染。

## 方案选择

采用混合方案（方案C）：catalog 定义是唯一事实来源，`catalog.prompt()` 生成 AI 提示词引导 LLM 输出正确 spec，前端 catalog + registry 解耦渲染。

## 组件体系

### 支持的组件（7个）

| 组件 | 类型 | 用途 | 子组件 |
|------|------|------|--------|
| Dashboard | 容器 | 根容器，定义标题和整体布局 | 任意 |
| Section | 容器 | 分组容器，支持行列布局 | 任意 |
| StatCard | 内容 | 统计指标卡片（数值+趋势） | 无 |
| Chart | 内容 | 图表（bar/line/pie/area），ECharts 渲染 | 无 |
| DataTable | 内容 | 数据列表 | 无 |
| TextBlock | 内容 | 文本/提示/说明（5种变体） | 无 |
| FlowChart | 内容 | 流程图（节点+边），ECharts 渲染 | 无 |

### Spec 格式

采用 json-render React schema 的元素树格式：

```json
{
  "root": "element_key",
  "elements": {
    "element_key": {
      "type": "ComponentName",
      "props": { ... },
      "children": ["child_key1", "child_key2"]
    }
  }
}
```

- `root`: 根元素 key
- `elements`: 扁平映射，key → 元素定义
- 每个元素: `type`(组件名) + `props`(属性) + `children`(子元素key数组)

## 架构设计

### 端到端数据流

```
CentralBrain 调用 OutputViewAgent
  → LLM 按需加载组件 skill 资源
  → LLM 输出 json-render spec（JSONL patch 逐行）
  → OutputViewEventHandlerHook 拦截 ActingChunkEvent
  → 发送 AGENT_OUTPUT Custom 事件（value = patch 字符串）
  → SSE 流式传输到前端
  → WebToolEventListener.onCustomEvent 接收
  → dispatchAgentOutput() pub/sub 分发
  → AiOutputPanel 监听
  → createSpecStreamCompiler.push(patch) 渐进编译
  → Renderer (catalog + registry) 渲染 7 组件
```

### 后端设计

#### OutputViewAgent

- 使用 SkillBox 注册单一 skill `output_view`
- `skill.md` 包含：组件总目录 + spec 格式规范 + 使用规则
- 组件详细文档作为资源文件放在 `resources/output-view/components/` 下，skill.md 中通过 `@components/xxx.md` 引用
- sysPrompt 中引用 catalog.prompt() 生成的提示词
- LLM 输出直接是 json-render spec JSON

#### OutputViewEventHandlerHook（已有，需调整）

- 拦截 OutputViewAgent 的 TextBlock 输出
- 将 textBlock.text 作为 AGENT_OUTPUT value 发送
- 当前实现已基本正确，需确保 value 格式为 JSONL patch 字符串

### 前端设计

#### services/agent-output/index.ts（新增）

pub/sub 模式，与现有 `human-approval`、`ask-user-question` 同构：

```typescript
type AgentOutputPayload = { patch: string };
// dispatchAgentOutput(payload) — 分发事件
// onAgentOutput(listener) — 注册监听，返回取消函数
```

#### CopilotKitProvider.tsx（修改）

在 `WebToolEventListener.onCustomEvent` 中新增 `AGENT_OUTPUT` 处理：

```typescript
if (event.name === 'AGENT_OUTPUT') {
  dispatchAgentOutput(event.value as AgentOutputPayload);
}
```

#### AiOutputPanel（重写）

去掉 iframe + postMessage 方案，改为：

1. 引入 json-render `Renderer` + `createSpecStreamCompiler`
2. 监听 `onAgentOutput` 事件，逐 patch 应用到 compiler
3. 用 `Renderer` 渲染 compiler 产出的 spec
4. 保留空状态展示
5. 新增清空功能
6. 主题化：所有组件样式使用项目 CSS 变量

#### Catalog + Registry（新增）

在 `AiOutputPanel/` 目录下创建：

- `catalog.ts` — defineCatalog，定义 7 个组件的 props schema（Zod）+ description
- `registry.tsx` — defineRegistry，7 个组件的 React 渲染实现
- `components/` — 各组件实现（StatCard、Chart、DataTable、TextBlock、FlowChart）
- `index.module.less` — 样式

### 关键设计决策

1. **后端不生成 HTML** — 只输出 spec JSON，前端负责渲染，支持主题切换
2. **catalog 是唯一事实来源** — 前端定义 catalog，`catalog.prompt()` 生成后端提示词
3. **JSONL patch 流式传输** — 逐行发送，前端 createSpecStreamCompiler 渐进渲染
4. **图表用 ECharts 渲染** — spec 只描述数据，渲染逻辑封装在组件内
5. **单一 Skill + 资源引用** — skill.md 是总目录含 spec 格式规范，组件详细文档作为资源按需加载

## 文件变更清单

### 后端新增

| 文件 | 说明 |
|------|------|
| `resources/output-view/skill.md` | Skill 总目录 + spec 格式规范 |
| `resources/output-view/components/dashboard.md` | Dashboard 组件详细文档 |
| `resources/output-view/components/section.md` | Section 组件详细文档 |
| `resources/output-view/components/stat-card.md` | StatCard 组件详细文档 |
| `resources/output-view/components/chart.md` | Chart 组件详细文档 |
| `resources/output-view/components/data-table.md` | DataTable 组件详细文档 |
| `resources/output-view/components/text-block.md` | TextBlock 组件详细文档 |
| `resources/output-view/components/flow-chart.md` | FlowChart 组件详细文档 |
| `resources/output-view/specs/examples.md` | 完整示例 |

### 后端修改

| 文件 | 说明 |
|------|------|
| `OutputViewAgent.java` | 完善 build()：注册 SkillBox + sysPrompt |

### 前端新增

| 文件 | 说明 |
|------|------|
| `services/agent-output/index.ts` | AgentOutput pub/sub 服务 |
| `AiOutputPanel/catalog.ts` | json-render catalog 定义 |
| `AiOutputPanel/registry.tsx` | json-render registry 定义 |
| `AiOutputPanel/components/Dashboard.tsx` | Dashboard 组件实现 |
| `AiOutputPanel/components/Section.tsx` | Section 组件实现 |
| `AiOutputPanel/components/StatCard.tsx` | StatCard 组件实现 |
| `AiOutputPanel/components/Chart.tsx` | Chart 组件实现（ECharts） |
| `AiOutputPanel/components/DataTable.tsx` | DataTable 组件实现 |
| `AiOutputPanel/components/TextBlock.tsx` | TextBlock 组件实现 |
| `AiOutputPanel/components/FlowChart.tsx` | FlowChart 组件实现（ECharts） |

### 前端修改

| 文件 | 说明 |
|------|------|
| `AiOutputPanel/index.tsx` | 重写：去掉 iframe，用 json-render Renderer |
| `AiOutputPanel/index.module.less` | 重写样式 |
| `CopilotKitProvider.tsx` | 新增 AGENT_OUTPUT 事件处理 |
