# StatCard 组件

统计指标卡片，展示单个核心指标及其趋势变化。通常在 Section(layout="row") 中并排展示多个。

## Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 指标名称，如"总事件数" |
| value | string | 是 | 指标值，如"1,284" |
| trend | "up" \| "down" \| "flat" | 否 | 趋势方向 |
| changeRate | string | 否 | 变化率，如"+12.5%" |
| icon | string | 否 | 图标名称（保留） |

## 示例

```json
{
  "type": "StatCard",
  "props": { "title": "总事件数", "value": "1,284", "trend": "up", "changeRate": "+12.5%" },
  "children": []
}
```
