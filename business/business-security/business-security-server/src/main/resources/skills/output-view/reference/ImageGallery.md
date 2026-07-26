# ImageGallery 组件

用于展示单张或多张图片，支持组件级标题/说明，以及每张图片自己的标题/说明。

## Props

```json
{
  "title": "方案截图",
  "description": "以下图片展示首页、列表页与详情页效果",
  "layout": "grid",
  "images": [
    {
      "url": "https://example.com/home.png",
      "title": "首页",
      "description": "突出总览信息和快速入口",
      "alt": "系统首页截图"
    },
    {
      "url": "https://example.com/detail.png",
      "title": "详情页",
      "description": "展示完整字段和关联信息",
      "alt": "详情页截图"
    }
  ]
}
```

## 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string \| null | 否 | 图片区块标题 |
| description | string \| null | 否 | 图片区块说明 |
| layout | `"grid"` \| `"carousel"` \| null | 否 | 布局模式。当前前端按网格渲染 |
| images | array | 是 | 图片列表 |
| images[].url | string | 是 | 图片地址 |
| images[].title | string \| null | 否 | 单张图片标题 |
| images[].description | string \| null | 否 | 单张图片说明 |
| images[].alt | string \| null | 否 | 替代文本 |

## JSONL 示例

````jsonl
```jsonl
{"op":"add","path":"/root","value":"dashboard"}
{"op":"add","path":"/elements/image-gallery-1","value":{"type":"ImageGallery","props":{"title":"界面预览","description":"展示 AI 生成的关键页面草图","layout":"grid","images":[{"url":"https://example.com/mockup-home.png","title":"首页","description":"概览区与快捷入口","alt":"首页草图"},{"url":"https://example.com/mockup-list.png","title":"列表页","description":"支持筛选与批量操作","alt":"列表页草图"}]},"children":[]}}
{"op":"add","path":"/elements/dashboard","value":{"type":"Dashboard","props":{"title":"设计输出","description":"AI 输出的图片结果"},"children":["image-gallery-1"]}}
```
````

## 推荐用法

- 单图场景：`images` 数组只放 1 项
- 多图场景：将相关图片放在同一个 `ImageGallery` 中，便于用户连续预览
- 如需文字上下文，优先用组件级 `title` / `description`
- 如需逐图解释，使用每个 `images[]` 项的 `title` / `description`

## 常见错误

1. 错误：使用 `src` 代替 `images`

```json
{ "src": "https://example.com/a.png" }
```

正确：

```json
{ "images": [{ "url": "https://example.com/a.png" }] }
```

2. 错误：把图片地址写成 `imageUrl`

```json
{ "images": [{ "imageUrl": "https://example.com/a.png" }] }
```

正确：

```json
{ "images": [{ "url": "https://example.com/a.png" }] }
```

3. 错误：把 `ImageGallery` 当容器组件使用，给它挂 `children`

正确做法：`ImageGallery` 是叶子组件，必须使用 `"children":[]`
