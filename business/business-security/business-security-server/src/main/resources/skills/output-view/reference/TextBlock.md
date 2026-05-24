# TextBlock 组件

文本/提示/说明组件，用于展示文字信息。

## 格式定义

```typescript
{
  type: "TextBlock",
  props: {
    content: string,                                // 必填，文本内容
    variant?: "plain" | "heading" | "info" | "warning" | "error"  // 可选，文本变体，默认 plain
  },
  children: []                                      // 叶子组件，children 必须为空数组
}
```

## 使用场景

- **plain**：普通文本说明
- **heading**：标题/强调文本
- **info**：提示信息（蓝色）
- **warning**：警告信息（黄色）
- **error**：错误信息（红色）

## 使用示例

```jsonl
{"op":"add","path":"/elements/text1","value":{"type":"TextBlock","props":{"content":"本月安全事件较上月增加12.5%，请关注异常登录行为。","variant":"info"},"children":[]}}
{"op":"add","path":"/elements/text2","value":{"type":"TextBlock","props":{"content":"检测到3个高风险接口未配置权限控制","variant":"warning"},"children":[]}}
{"op":"add","path":"/elements/text3","value":{"type":"TextBlock","props":{"content":"数据权限同步失败，请检查Redis连接","variant":"error"},"children":[]}}
{"op":"add","path":"/elements/text4","value":{"type":"TextBlock","props":{"content":"以上数据统计截止至2024年5月22日"},"children":[]}}
```
