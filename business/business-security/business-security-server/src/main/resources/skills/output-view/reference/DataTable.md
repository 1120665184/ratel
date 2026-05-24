# DataTable 组件

数据列表组件，展示结构化表格数据。

## 格式定义

```typescript
{
  type: "DataTable",
  props: {
    title?: string | null,                          // 可选，表格标题
    columns: {                                      // 必填，列定义
      key: string,                                  // 列字段名，对应 data 中的 key
      label: string,                                // 列标题，显示在表头
      width?: string | null                         // 可选，列宽度
    }[],
    data: Record<string, string | number>[],        // 必填，行数据数组
    bordered?: boolean | null,                      // 可选，是否显示边框
    striped?: boolean | null                        // 可选，是否显示斑马纹
  },
  children: []                                      // 叶子组件，children 必须为空数组
}
```

## 使用场景

- 展示结构化的明细数据，如模块接口清单、事件列表、用户列表
- 适合展示多列、多行的表格数据
- 每列都需要有明确的 key 和 label

## ⚠️ 关键格式要求

1. **`columns` 必须是对象数组 `[{ key, label }]`**，禁止使用字符串数组
2. **每个列必须同时包含 `key` 和 `label`**，key 对应数据字段，label 为显示标题
3. **`data` 中每行的字段名必须与 `columns` 中的 `key` 一一对应**
4. **所有值都必须是字符串或数字**，禁止嵌套数组或对象
5. **如果原始数据中某字段是数组（如 methods、paths）**，必须用逗号拼接为字符串

## 使用示例

基础表格：

```jsonl
{"op":"add","path":"/elements/t1","value":{"type":"DataTable","props":{"title":"近期事件","columns":[{"key":"time","label":"时间"},{"key":"type","label":"类型"},{"key":"level","label":"级别"}],"data":[{"time":"05-22 14:30","type":"登录异常","level":"高"},{"time":"05-22 10:15","type":"权限变更","level":"中"}],"bordered":true,"striped":true},"children":[]}}
```

含数组字段的表格（methods、paths 等数组需拼接为字符串）：

```jsonl
{"op":"add","path":"/elements/t2","value":{"type":"DataTable","props":{"title":"模块接口清单","columns":[{"key":"module","label":"模块"},{"key":"count","label":"接口数"},{"key":"methods","label":"请求方式"},{"key":"paths","label":"路径"}],"data":[{"module":"用户管理","count":9,"methods":"POST, GET, DELETE, PUT","paths":"/manager, /dept/tree, /user-dept"},{"module":"角色管理","count":18,"methods":"GET, POST, DELETE, PUT","paths":"/role/page, /role, /role/tree"}],"bordered":true,"striped":true},"children":[]}}
```

## 常见错误

```jsonl
// ❌ 错误：columns 使用字符串数组
{"type":"DataTable","props":{"columns":["module","count","methods"],"data":[...]},"children":[]}

// ❌ 错误：columns 中省略 label
{"type":"DataTable","props":{"columns":[{"key":"module"},{"key":"count"}],"data":[...]},"children":[]}

// ❌ 错误：data 中嵌套数组
{"type":"DataTable","props":{"columns":[{"key":"module","label":"模块"},{"key":"methods","label":"请求方式"}],"data":[{"module":"用户管理","methods":["POST","GET","DELETE"]}],"children":[]}

// ✅ 正确：数组拼接为字符串
{"type":"DataTable","props":{"columns":[{"key":"module","label":"模块"},{"key":"methods","label":"请求方式"}],"data":[{"module":"用户管理","methods":"POST, GET, DELETE"}],"children":[]}
```
