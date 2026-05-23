# FlowChart 组件

流程图组件，展示流程和决策路径。

## Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 否 | 流程图标题 |
| direction | "vertical" \| "horizontal" | 否 | 布局方向，默认 vertical |
| nodes | array | 是 | 流程节点列表 |
| edges | array | 是 | 节点连线列表 |

## nodes 结构

```json
[
  { "id": "start", "label": "事件上报", "type": "start" },
  { "id": "review", "label": "安全评审", "type": "process" },
  { "id": "approve", "label": "审批通过", "type": "decision" },
  { "id": "end", "label": "归档完成", "type": "end" }
]
```

节点类型：
- `start`: 开始节点（绿色圆形）
- `process`: 处理节点（蓝色圆角矩形）
- `decision`: 判断节点（黄色菱形）
- `end`: 结束节点（红色圆形）

## edges 结构

```json
[
  { "source": "start", "target": "review" },
  { "source": "approve", "target": "handle", "label": "是" },
  { "source": "approve", "target": "review", "label": "否" }
]
```

## 示例

```json
{
  "type": "FlowChart",
  "props": {
    "title": "安全事件处理流程",
    "direction": "vertical",
    "nodes": [
      { "id": "start", "label": "事件上报", "type": "start" },
      { "id": "review", "label": "安全评审", "type": "process" },
      { "id": "end", "label": "归档完成", "type": "end" }
    ],
    "edges": [
      { "source": "start", "target": "review" },
      { "source": "review", "target": "end" }
    ]
  },
  "children": []
}
```
