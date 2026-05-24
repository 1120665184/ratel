# StatCard 组件

统计指标卡片组件，展示单个核心指标及其趋势。

## 格式定义

```typescript
{
  type: "StatCard",
  props: {
    title: string,                    // 必填，指标名称，如"总事件数"
    value: string,                    // 必填，指标值，如"1,284"
    trend?: "up" | "down" | "flat",  // 可选，趋势方向
    changeRate?: string,              // 可选，变化率，如"+12.5%"
    icon?: string                     // 可选，图标名称
  },
  children: []                        // 叶子组件，children 必须为空数组
}
```

## 使用场景

- 展示关键数值指标，如总数、占比、增长率
- 通常在 `Section(layout="row")` 中并排展示 2-4 个 StatCard
- 每个 StatCard 只展示一个指标

## 使用示例

```jsonl
{"op":"add","path":"/elements/card-1","value":{"type":"StatCard","props":{"title":"总事件数","value":"1,284","trend":"up","changeRate":"+12.5%"},"children":[]}}
{"op":"add","path":"/elements/card-2","value":{"type":"StatCard","props":{"title":"已处理","value":"1,156","trend":"up","changeRate":"+8.3%"},"children":[]}}
{"op":"add","path":"/elements/card-3","value":{"type":"StatCard","props":{"title":"待处理","value":"128","trend":"down","changeRate":"-5.2%"},"children":[]}}
```

配合 Section 并排展示：

```jsonl
{"op":"add","path":"/elements/s1","value":{"type":"Section","props":{"title":"核心指标","layout":"row"},"children":["card-1","card-2","card-3"]}}
```

## 常见错误

```jsonl
// ❌ 错误：value 必须是字符串，不能是数字
{"type":"StatCard","props":{"title":"总数","value":1284},"children":[]}

// ❌ 错误：trend 必须是枚举值，不能用箭头符号
{"type":"StatCard","props":{"title":"总数","value":"1,284","trend":"↑"},"children":[]}

// ✅ 正确
{"type":"StatCard","props":{"title":"总数","value":"1,284","trend":"up"},"children":[]}
```
