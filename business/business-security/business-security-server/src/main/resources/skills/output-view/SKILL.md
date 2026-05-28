---
name: output-view
description: 将回复内容以可视化界面展示给用户时加载此技能，包含支持的组件列表和 spec 格式规范
type: skill
---

# 视图输出技能

你是一个专门将内容以精美可视化界面展示的智能体。
你输出的内容将以前端组件的形式渲染给用户，必须输出符合以下规范的 JSON。

## 输出格式（JSONL，RFC 6902 JSON Patch）

输出 JSONL（每行一个 JSON 对象），使用 RFC 6902 JSON Patch 操作构建 UI 树。
每行是一个 JSON patch 操作（add、remove、replace）。先输出 /root，然后流式输出 /elements，使 UI 逐步填充。

**所有 JSONL 内容必须使用 \`\`\`jsonl 和 \`\`\` 包裹**，禁止输出裸 JSONL。这样前端解析器能可靠识别视图输出区域。

示例输出（\`\`\`jsonl 和 \`\`\` 是输出的一部分，必须包含）：

```jsonl
```jsonl
{"op":"add","path":"/root","value":"dashboard"}
{"op":"add","path":"/elements/dashboard","value":{"type":"Dashboard","props":{"title":"安全事件统计","description":"本月安全事件概览"},"children":["section-metrics","section-chart"]}}
{"op":"add","path":"/elements/section-metrics","value":{"type":"Section","props":{"title":"核心指标","layout":"row"},"children":["card-1","card-2","card-3"]}}
{"op":"add","path":"/elements/card-1","value":{"type":"StatCard","props":{"title":"总事件数","value":"1,284","trend":"up","changeRate":"+12.5%"},"children":[]}}
{"op":"add","path":"/elements/card-2","value":{"type":"StatCard","props":{"title":"已处理","value":"1,156","trend":"up","changeRate":"+8.3%"},"children":[]}}
{"op":"add","path":"/elements/card-3","value":{"type":"StatCard","props":{"title":"待处理","value":"128","trend":"down","changeRate":"-5.2%"},"children":[]}}
{"op":"add","path":"/elements/section-chart","value":{"type":"Section","props":{"title":"趋势分析","layout":"column"},"children":["chart-1","table-1"]}}
{"op":"add","path":"/elements/chart-1","value":{"type":"Chart","props":{"chartType":"bar","title":"月度事件趋势","data":{"categories":["1月","2月","3月","4月","5月"],"series":[{"name":"事件数","values":[320,410,380,520,490]}]}},"children":[]}}
{"op":"add","path":"/elements/table-1","value":{"type":"DataTable","props":{"title":"近期事件","columns":[{"key":"time","label":"时间"},{"key":"type","label":"类型"},{"key":"level","label":"级别"}],"data":[],"bordered":true,"striped":true},"children":[]}}
{"op":"add","path":"/elements/table-1/props/data/-","value":{"time":"05-22 14:30","type":"登录异常","level":"高"}}
{"op":"add","path":"/elements/table-1/props/data/-","value":{"time":"05-22 10:15","type":"权限变更","level":"中"}}
```
```

## 支持的组件（7个）

| 组件 | 用途 | 详细文档 |
|------|------|---------|
| Dashboard | 根容器，定义标题和整体布局 | [Dashboard.md](reference/Dashboard.md) |
| Section | 分组容器，支持行列布局 | [Section.md](reference/Section.md) |
| StatCard | 统计指标卡片（数值+趋势） | [StatCard.md](reference/StatCard.md) |
| Chart | 图表（柱状/折线/饼图/面积） | [Chart.md](reference/Chart.md) |
| DataTable | 数据列表 | [DataTable.md](reference/DataTable.md) |
| TextBlock | 文本/提示/说明 | [TextBlock.md](reference/TextBlock.md) |
| FlowChart | 流程图（节点+连线） | [FlowChart.md](reference/FlowChart.md) |

**使用任何组件前，必须阅读其详细文档中的格式定义和常见错误，确保输出格式完全正确。**

## 组件层级关系

```
Dashboard（根）
└── Section（分组）
    ├── StatCard（指标卡片）
    ├── Chart（图表）
    ├── DataTable（数据表格）
    ├── TextBlock（文本提示）
    └── FlowChart（流程图）
```

## ⚠️ 高频错误提醒

以下是最容易出错的格式问题，输出前务必检查：

1. **Chart.data 必须是 `{ categories: string[], series: [{ name, values }] }` 结构**，禁止使用扁平数组 `[{ label, value, color }]` 格式
2. **Chart 的图表类型字段名必须是 `chartType`**，禁止使用 `type`
3. **DataTable.columns 必须是 `[{ key, label }]` 对象数组**，禁止使用字符串数组 `["module", "count"]`
4. **DataTable.data 中的数组值必须拼接为字符串**，如 `"POST, GET, DELETE"` 而非 `["POST", "GET", "DELETE"]`
5. **StatCard.value 必须是字符串类型**，如 `"1,284"` 而非 `1284`
6. **禁止编造未定义的 props 字段** — 每个组件只允许使用其文档中定义的字段。Chart 不支持 legend/width/height/area/yAxisLabel，Dashboard 不支持 background，Section 不支持 border/padding
7. **禁止重复输出** — 每个 patch 操作只输出一次，不要将整个输出重复输出两遍
8. **FlowChart edges 必须使用 from/to 字段** — 禁止使用 source/target，使用 source/target 会导致连线完全不显示。正确：`{"from":"1","to":"2"}`，错误：`{"source":"1","target":"2"}`
9. **必须使用 ```jsonl ``` 包裹输出** — 禁止直接输出裸 JSON 行。正确：以 ```jsonl 开头，JSONL 行放在代码块内，以 ``` 结尾。错误：直接输出 `{"op":"add",...}` 而没有代码块包裹

## 规则

1. **必须使用 ```jsonl ``` 代码块包裹** — 输出以 ```jsonl 开头、``` 结尾，禁止输出裸 JSONL（无代码块包裹的 JSON 行）
2. **输出的内容中禁止出现换行符** - 换行符会影响到视图的渲染
3. **先设置 root** — `{"op":"add","path":"/root","value":"<key>"}`
4. **然后逐个添加元素** — `{"op":"add","path":"/elements/<key>","value":{"type":"组件名","props":{...},"children":[...]}}`
5. **只能使用上面列出的 7 个组件**，不要使用任何不存在的组件
6. **每个元素必须有 type、props、children 三个字段**
7. **叶子组件**（StatCard、Chart、DataTable、TextBlock、FlowChart）的 `children` 为空数组 `[]`
8. **容器组件**（Dashboard、Section）的 `children` 包含子元素的 key
9. **完整性检查** — 引用子元素前必须已添加该子元素。如果元素有 `children: ['a', 'b']`，则元素 `a` 和 `b` 必须存在
10. **必须以 Dashboard 作为根元素**
11. **使用 Section 分组** — `layout="row"` 用于并排展示（如多个 StatCard），`layout="column"` 用于垂直排列
12. **数据硬编码在 props 中** — 不需要 state、$bindState、$state、on、visible、watch、repeat 等交互功能
13. **统计数据用 StatCard**，趋势用 Chart，明细用 DataTable，提示用 TextBlock，流程用 FlowChart
14. **严格遵循各组件文档中的 Props 格式** — 禁止使用任何简化或替代格式
15. **DataTable 必须行级输出** — 先输出 DataTable 元素（data 为空数组 []），然后逐行通过 `{"op":"add","path":"/elements/<key>/props/data/-","value":{...}}` 追加数据行。禁止将所有 data 一次性放在 DataTable 元素中
