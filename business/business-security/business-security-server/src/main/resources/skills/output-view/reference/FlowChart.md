# FlowChart 组件

流程图组件，展示流程和决策路径。

## 格式定义

```typescript
{
  type: "FlowChart",
  props: {
    title?: string | null,                          // 可选，流程图标题
    direction?: "vertical" | "horizontal" | null,   // 可选，布局方向，默认 vertical
    nodes: {                                        // 必填，流程节点列表
      id: string,                                   // 节点唯一标识
      label: string,                                // 节点显示文本
      type: "start" | "process" | "decision" | "end"  // 节点类型
    }[],
    edges: {                                        // 必填，节点连线列表
      from: string,                                 // 起始节点 id
      to: string,                                   // 目标节点 id
      label?: string | null                         // 可选，边标签
    }[]
  },
  children: []                                      // 叶子组件，children 必须为空数组
}
```

## 使用场景

- 展示业务流程、审批流程、处理步骤
- **start**：流程起点
- **process**：处理步骤
- **decision**：判断/决策节点（通常有多个出边，用 label 区分）
- **end**：流程终点

## 使用示例

简单流程：

```jsonl
{"op":"add","path":"/elements/flow1","value":{"type":"FlowChart","props":{"title":"事件处理流程","direction":"vertical","nodes":[{"id":"start","label":"事件上报","type":"start"},{"id":"review","label":"安全评审","type":"process"},{"id":"approve","label":"审批通过","type":"decision"},{"id":"end","label":"归档完成","type":"end"}],"edges":[{"from":"start","to":"review"},{"from":"review","to":"approve"},{"from":"approve","to":"end","label":"是"},{"from":"approve","to":"review","label":"否"}]},"children":[]}}
```

## 常见错误

### 节点字段错误

```jsonl
// ❌ 错误：使用 name 而非 label 作为节点显示文本
{"id":"n1","name":"步骤1","type":"process"}

// ✅ 正确：必须使用 label 字段
{"id":"n1","label":"步骤1","type":"process"}
```

### 节点类型错误

```jsonl
// ❌ 错误：使用 BPMN/工作流风格类型名（startEvent、userTask、endEvent、serviceTask 等）
{"id":"n1","label":"提交申请","type":"startEvent"}
{"id":"n2","label":"审批","type":"userTask"}
{"id":"n3","label":"结束","type":"endEvent"}

// ✅ 正确：只允许 start、process、decision、end 四种类型
{"id":"n1","label":"提交申请","type":"start"}
{"id":"n2","label":"审批","type":"process"}
{"id":"n3","label":"结束","type":"end"}
```

节点类型映射规则：
| BPMN/工作流类型 | 正确类型 |
|----------------|---------|
| startEvent | start |
| userTask / serviceTask / scriptTask | process |
| exclusiveGateway / parallelGateway | decision |
| endEvent | end |

### 连线字段错误

```jsonl
// ❌ 错误：使用 source/target 而非 from/to
{"source":"start","target":"step1"}

// ❌ 错误：使用 transitions 而非 edges
{"transitions":[{"from":"start","to":"step1"}]}

// ✅ 正确：必须使用 edges 数组，字段为 from 和 to
{"edges":[{"from":"start","to":"step1"}]}
```

### 引用完整性错误

```jsonl
// ❌ 错误：edges 引用不存在的节点 id
{"from":"start","to":"non-existent"}

// ✅ 正确：所有 edges 引用的节点 id 必须在 nodes 中定义
{"nodes":[{"id":"start","label":"开始","type":"start"},{"id":"end","label":"结束","type":"end"}],"edges":[{"from":"start","to":"end"}]}
```

### 禁止添加额外字段

```jsonl
// ❌ 错误：添加 catalog 未定义的额外字段（status、time、icon、assignee、comment 等）
{"id":"n1","label":"审批","type":"process","status":"completed","assignee":"张三","time":"2024-01-15","icon":"✅","comment":"同意"}

// ✅ 正确：只包含 id、label、type 三个字段，额外信息可通过 edges 的 label 传达
{"id":"n1","label":"审批","type":"process"}
```

### 树结构错误

```jsonl
// ❌ 错误：FlowChart 所在的 Section 未挂载到 Dashboard 的 children 中
// Dashboard children 缺少 "s2"，导致 FlowChart 成为孤儿节点不可达
{"op":"add","path":"/elements/d1","value":{"type":"Dashboard","props":{"title":"示例"},"children":["s1"]}}
{"op":"add","path":"/elements/s2","value":{"type":"Section","props":{"title":"流程图","layout":"column"},"children":["fc1"]}}
{"op":"add","path":"/elements/fc1","value":{"type":"FlowChart","props":{...},"children":[]}}

// ✅ 正确：Dashboard 的 children 必须包含所有需要展示的 Section
{"op":"add","path":"/elements/d1","value":{"type":"Dashboard","props":{"title":"示例"},"children":["s1","s2"]}}
{"op":"add","path":"/elements/s2","value":{"type":"Section","props":{"title":"流程图","layout":"column"},"children":["fc1"]}}
{"op":"add","path":"/elements/fc1","value":{"type":"FlowChart","props":{...},"children":[]}}
```
