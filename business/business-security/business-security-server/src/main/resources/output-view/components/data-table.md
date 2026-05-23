# DataTable 组件

数据列表组件，展示结构化表格数据。

## Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 否 | 表格标题 |
| columns | array | 是 | 列定义数组 |
| data | array | 是 | 行数据数组 |
| bordered | boolean | 否 | 是否显示边框，默认 true |
| striped | boolean | 否 | 是否显示斑马纹，默认 true |

## columns 结构

```json
[
  { "key": "name", "label": "名称" },
  { "key": "value", "label": "值", "width": "120px" }
]
```

## data 结构

每行是一个 key-value 对象，key 对应 columns 中的 key：

```json
[
  { "name": "登录异常", "value": "23" },
  { "name": "权限变更", "value": "15" }
]
```

## 示例

```json
{
  "type": "DataTable",
  "props": {
    "title": "近期事件",
    "columns": [
      { "key": "time", "label": "时间" },
      { "key": "type", "label": "类型" },
      { "key": "level", "label": "级别" }
    ],
    "data": [
      { "time": "05-22 14:30", "type": "登录异常", "level": "高" },
      { "time": "05-22 10:15", "type": "权限变更", "level": "中" }
    ],
    "bordered": true,
    "striped": true
  },
  "children": []
}
```
