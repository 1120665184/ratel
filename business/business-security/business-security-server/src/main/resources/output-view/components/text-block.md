# TextBlock 组件

文本/提示/说明组件，支持 5 种变体。

## Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | string | 是 | 文本内容 |
| variant | "plain" \| "heading" \| "info" \| "warning" \| "error" | 否 | 文本变体，默认 plain |

## variant 说明

| 变体 | 用途 | 视觉效果 |
|------|------|----------|
| plain | 普通正文 | 默认文本样式 |
| heading | 小标题 | 加粗 + 左侧蓝色竖条 |
| info | 提示信息 | 蓝色背景 + 蓝色竖条 |
| warning | 警告信息 | 黄色背景 + 黄色竖条 |
| error | 错误信息 | 红色背景 + 红色竖条 |

## 示例

```json
{
  "type": "TextBlock",
  "props": { "content": "数据统计周期为2026年5月1日至5月22日", "variant": "info" },
  "children": []
}
```
