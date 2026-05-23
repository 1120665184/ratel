# Dashboard 组件

根容器组件，定义仪表盘标题和整体布局。必须作为最外层元素使用。

## Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 仪表盘标题 |
| description | string | 否 | 仪表盘描述文字 |

## 示例

```json
{
  "type": "Dashboard",
  "props": { "title": "安全事件统计", "description": "本月安全事件概览" },
  "children": ["section1", "section2"]
}
```
