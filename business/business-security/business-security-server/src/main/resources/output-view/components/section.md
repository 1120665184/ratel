# Section 组件

分组容器组件，支持行列布局。用于将内容按主题分组。

## Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 否 | 分组标题 |
| description | string | 否 | 分组描述 |
| layout | "row" \| "column" | 否 | 子元素排列方式，默认 column |

## 示例

```json
{
  "type": "Section",
  "props": { "title": "核心指标", "layout": "row" },
  "children": ["card1", "card2", "card3"]
}
```
