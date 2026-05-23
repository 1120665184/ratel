# 完整示例

## 数据统计仪表盘

```json
{
  "root": "d1",
  "elements": {
    "d1": {
      "type": "Dashboard",
      "props": { "title": "安全事件统计", "description": "本月安全事件概览" },
      "children": ["s1", "s2"]
    },
    "s1": {
      "type": "Section",
      "props": { "title": "核心指标", "layout": "row" },
      "children": ["sc1", "sc2", "sc3"]
    },
    "sc1": {
      "type": "StatCard",
      "props": { "title": "总事件数", "value": "1,284", "trend": "up", "changeRate": "+12.5%" },
      "children": []
    },
    "sc2": {
      "type": "StatCard",
      "props": { "title": "已处理", "value": "1,156", "trend": "up", "changeRate": "+8.3%" },
      "children": []
    },
    "sc3": {
      "type": "StatCard",
      "props": { "title": "待处理", "value": "128", "trend": "down", "changeRate": "-5.2%" },
      "children": []
    },
    "s2": {
      "type": "Section",
      "props": { "title": "趋势分析", "layout": "column" },
      "children": ["c1", "t1"]
    },
    "c1": {
      "type": "Chart",
      "props": {
        "chartType": "bar",
        "title": "月度事件趋势",
        "data": {
          "categories": ["1月", "2月", "3月", "4月", "5月"],
          "series": [{ "name": "事件数", "values": [320, 410, 380, 520, 490] }]
        }
      },
      "children": []
    },
    "t1": {
      "type": "DataTable",
      "props": {
        "title": "近期事件",
        "columns": [
          { "key": "time", "label": "时间" },
          { "key": "type", "label": "类型" },
          { "key": "level", "label": "级别" },
          { "key": "status", "label": "状态" }
        ],
        "data": [
          { "time": "05-22 14:30", "type": "登录异常", "level": "高", "status": "已处理" },
          { "time": "05-22 10:15", "type": "权限变更", "level": "中", "status": "待处理" }
        ],
        "bordered": true,
        "striped": true
      },
      "children": []
    }
  }
}
```
