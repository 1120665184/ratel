# Chart 组件

图表组件，支持柱状图、折线图、饼图、面积图。

## 格式定义

```typescript
{
  type: "Chart",
  props: {
    chartType: "bar" | "line" | "pie" | "area",   // 必填，图表类型
    title?: string | null,                          // 可选，图表标题
    data: {                                         // 必填，图表数据
      categories: string[],                         // X 轴分类标签 / 饼图扇区名称
      series: {                                     // 数据系列
        name: string,                               // 系列名称
        values: number[]                            // 系列数据值，与 categories 一一对应
      }[]
    }
  },
  children: []                                      // 叶子组件，children 必须为空数组
}
```

## 使用场景

- **柱状图 (bar)**：比较不同分类的数值大小，如各模块接口数量
- **折线图 (line)**：展示趋势变化，如月度事件趋势
- **饼图 (pie)**：展示占比分布，如中心接口占比
- **面积图 (area)**：展示趋势及累计量，如流量趋势

## ⚠️ 关键格式要求

1. **图表类型字段名必须是 `chartType`**，禁止使用 `type`
2. **`data` 必须是 `{ categories, series }` 结构**，禁止使用扁平数组
3. **`series[].values` 必须是 `number[]`**，禁止使用字符串数字
4. **`categories` 和 `series[].values` 长度必须一致**

## 使用示例

柱状图：

```jsonl
{"op":"add","path":"/elements/chart1","value":{"type":"Chart","props":{"chartType":"bar","title":"各模块接口数量","data":{"categories":["用户管理","部门管理","角色管理","菜单管理","数据权限","AI表模型"],"series":[{"name":"接口数","values":[9,7,18,6,5,11]}]}},"children":[]}}
```

折线图：

```jsonl
{"op":"add","path":"/elements/chart2","value":{"type":"Chart","props":{"chartType":"line","title":"月度事件趋势","data":{"categories":["1月","2月","3月","4月","5月"],"series":[{"name":"事件数","values":[320,410,380,520,490]}]}},"children":[]}}
```

饼图：

```jsonl
{"op":"add","path":"/elements/chart3","value":{"type":"Chart","props":{"chartType":"pie","title":"中心接口占比","data":{"categories":["用户中心","安全中心"],"series":[{"name":"接口数","values":[16,53]}]}},"children":[]}}
```

多系列柱状图：

```jsonl
{"op":"add","path":"/elements/chart4","value":{"type":"Chart","props":{"chartType":"bar","title":"月度对比","data":{"categories":["1月","2月","3月"],"series":[{"name":"事件数","values":[320,410,380]},{"name":"已处理","values":[300,390,370]}]}},"children":[]}}
```

## 常见错误

```jsonl
// ❌ 错误：使用 type 而非 chartType
{"type":"Chart","props":{"type":"bar","data":{...}},"children":[]}

// ❌ 错误：data 使用扁平数组格式
{"type":"Chart","props":{"chartType":"bar","data":[{"module":"用户管理","count":9},{"module":"角色管理","count":18}]},"children":[]}

// ❌ 错误：data 使用扁平数组 + xAxis/yAxis
{"type":"Chart","props":{"chartType":"bar","data":[{"module":"用户管理","count":9}],"xAxis":"模块","yAxis":"接口数"},"children":[]}

// ❌ 错误：values 使用字符串
{"type":"Chart","props":{"chartType":"bar","data":{"categories":["A","B"],"series":[{"name":"数量","values":["9","18"]}]}},"children":[]}

// ✅ 正确
{"type":"Chart","props":{"chartType":"bar","data":{"categories":["用户管理","角色管理"],"series":[{"name":"接口数","values":[9,18]}]}},"children":[]}
```
