# Chart 组件

图表组件，支持柱状图、折线图、饼图、面积图。

## Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chartType | "bar" \| "line" \| "pie" \| "area" | 是 | 图表类型 |
| title | string | 否 | 图表标题 |
| data | object | 是 | 图表数据 |

## data 结构

```json
{
  "categories": ["1月", "2月", "3月"],
  "series": [
    { "name": "事件数", "values": [320, 410, 380] }
  ]
}
```

- `categories`: X 轴分类标签数组
- `series`: 数据系列数组，每个系列有 name 和 values
- 饼图时 categories 作为扇区标签，series 只有一个系列

## 示例

```json
{
  "type": "Chart",
  "props": {
    "chartType": "bar",
    "title": "月度事件趋势",
    "data": {
      "categories": ["1月", "2月", "3月"],
      "series": [{ "name": "事件数", "values": [320, 410, 380] }]
    }
  },
  "children": []
}
```
