# Section 组件

分组容器组件，支持行列布局，用于将内容按主题分组。

## 格式定义

```typescript
{
  type: "Section",
  props: {
    title?: string | null,                       // 可选，分组标题
    description?: string | null,                 // 可选，分组描述
    layout?: "row" | "column" | null             // 可选，子元素排列方式，默认 column
  },
  children: string[]                             // 子元素 key 数组
}
```

## 使用场景

- Dashboard 的直接子元素，用于按主题划分区域
- `layout="row"`：子元素水平排列，适合并排展示多个 StatCard
- `layout="column"`：子元素垂直排列（默认），适合依次展示 Chart、DataTable 等

## 使用示例

水平排列多个 StatCard：

```jsonl
{"op":"add","path":"/elements/s1","value":{"type":"Section","props":{"title":"核心指标","layout":"row"},"children":["card-1","card-2","card-3"]}}
```

垂直排列图表和表格：

```jsonl
{"op":"add","path":"/elements/s2","value":{"type":"Section","props":{"title":"趋势分析","layout":"column"},"children":["chart-1","table-1"]}}
```

图表与子分组并排展示：

```jsonl
{"op":"add","path":"/elements/s3","value":{"type":"Section","props":{"title":"模块接口分布","layout":"row"},"children":["chart-1","section-detail"]}}
```
