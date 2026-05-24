# Dashboard 组件

根容器组件，定义仪表盘标题和整体布局，**必须作为最外层元素使用**。

## 格式定义

```typescript
{
  type: "Dashboard",
  props: {
    title: string,           // 必填，仪表盘标题
    description?: string     // 可选，仪表盘描述
  },
  children: string[]         // 子元素 key 数组，引用 Section 组件
}
```

## 使用场景

- 每个可视化输出**必须且只能**有一个 Dashboard 作为根元素
- Dashboard 只能包含 Section 作为直接子元素
- 通过嵌套不同 layout 的 Section 来组织整体布局

## 使用示例

```jsonl
{"op":"add","path":"/root","value":"dashboard"}
{"op":"add","path":"/elements/dashboard","value":{"type":"Dashboard","props":{"title":"安全事件统计","description":"本月安全事件概览"},"children":["section-metrics","section-chart"]}}
```

## 常见错误

```jsonl
// ❌ 错误：Dashboard 不能作为非根元素
{"op":"add","path":"/elements/inner","value":{"type":"Dashboard","props":{"title":"子仪表盘"},"children":[]}}

// ❌ 错误：Dashboard 的 children 不能直接包含 StatCard、Chart 等叶子组件
{"op":"add","path":"/elements/d1","value":{"type":"Dashboard","props":{"title":"仪表盘"},"children":["card-1"]}}

// ✅ 正确：Dashboard 只包含 Section
{"op":"add","path":"/elements/d1","value":{"type":"Dashboard","props":{"title":"仪表盘"},"children":["section-1","section-2"]}}
```
