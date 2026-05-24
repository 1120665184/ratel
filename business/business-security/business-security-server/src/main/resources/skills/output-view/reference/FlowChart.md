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
      source: string,                               // 起始节点 id
      target: string,                               // 目标节点 id
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
{"op":"add","path":"/elements/flow1","value":{"type":"FlowChart","props":{"title":"事件处理流程","direction":"vertical","nodes":[{"id":"start","label":"事件上报","type":"start"},{"id":"review","label":"安全评审","type":"process"},{"id":"approve","label":"审批通过","type":"decision"},{"id":"end","label":"归档完成","type":"end"}],"edges":[{"source":"start","target":"review"},{"source":"review","target":"approve"},{"source":"approve","target":"end","label":"是"},{"source":"approve","target":"review","label":"否"}]},"children":[]}}
```

## 常见错误

```jsonl
// ❌ 错误：nodes 中使用不存在的 type
{"id":"n1","label":"步骤","type":"step"}

// ❌ 错误：edges 引用不存在的节点 id
{"source":"start","target":"non-existent"}

// ✅ 正确：所有 edges 引用的节点 id 必须在 nodes 中定义
{"nodes":[{"id":"start","label":"开始","type":"start"},{"id":"end","label":"结束","type":"end"}],"edges":[{"source":"start","target":"end"}]}
```
