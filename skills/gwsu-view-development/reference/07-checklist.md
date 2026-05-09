# 七、开发检查清单

## 7.1 新建子应用

- [ ] 在 `apps/` 下创建应用目录
- [ ] 配置 `config/config.ts`：设置 `base`、`mountElementId`、`esbuildMinifyIIFE: true`、`qiankun: { slave: {} }`
- [ ] 配置 `config/routes.ts`：定义路由结构
- [ ] 布局组件使用 `ThemeLayout` 包裹 `<Outlet />`
- [ ] 添加到 `pnpm-workspace.yaml`
- [ ] 在主应用 `config/config.ts` 的 `qiankun.master.apps` 中注册
- [ ] 在主应用 `config/routes.ts` 添加 `microApp` 路由
- [ ] 在根 `package.json` 添加 dev/build 脚本
- [ ] `mountElementId` 与其他子应用不重复

## 7.2 新建业务页面

- [ ] 在 `src/pages/` 下创建页面目录
- [ ] 创建 `index.tsx` 页面主组件
- [ ] 创建 `index.module.less` 页面样式文件
- [ ] 在 `config/routes.ts` 中注册路由
- [ ] 页面组件使用函数式组件（`React.FC` 或 `export default function`）
- [ ] API 路径包含正确的模块前缀（如 `/system/`、`/security/`）
- [ ] 样式使用 `import styles from './index.module.less'`，不使用 CSS-in-JS
- [ ] 复杂逻辑抽离为自定义 Hooks（`hooks/useXxx.tsx`）
- [ ] 类型定义放在 `types/index.ts`

## 7.3 新建组件

- [ ] 在 `components/` 下创建组件目录（PascalCase）
- [ ] 创建 `index.tsx` 组件文件
- [ ] 创建 `index.module.less` 样式文件
- [ ] 定义清晰的 Props interface
- [ ] 样式使用 CSS 变量引用主题色（`var(--primary-color)` 等）
- [ ] 组件导出使用 `export default`
- [ ] 事件回调使用 `useCallback` 包裹

## 7.4 新建 API 服务

- [ ] 服务文件放在 `services/` 或 `pages/{业务}/services/`
- [ ] 使用 `@gwsu/core` 的 `get/post/put/del` 方法
- [ ] API 路径包含正确的模块前缀
- [ ] 函数有 JSDoc 注释
- [ ] 返回值类型明确
- [ ] 服务函数命名遵循规范（getXxx / saveOrUpdateXxx / deleteXxx）

## 7.5 样式检查

- [ ] 样式文件为 `*.module.less` 格式
- [ ] 未使用 CSS-in-JS 或内联样式（动态 style 除外）
- [ ] 颜色值使用 CSS 变量（`var(--xxx-color)`）而非硬编码
- [ ] Less 嵌套层级不超过 4 层
- [ ] 类名使用 camelCase

## 7.6 主题兼容性检查

- [ ] 页面在亮色主题下正常显示
- [ ] 页面在暗色主题（midnight）下正常显示
- [ ] 颜色值未硬编码，使用 CSS 变量
- [ ] Ant Design 组件未覆盖主题 token
- [ ] 图片和图标在暗色模式下可见

## 7.7 微前端兼容性检查

- [ ] 子应用使用 `ThemeLayout` 包裹
- [ ] 子应用 `base` 路径与主应用路由前缀一致
- [ ] `mountElementId` 唯一不冲突
- [ ] 跨应用通信使用 `emitEvent` / `onEvent`
- [ ] localStorage key 使用 `gwsu_` 前缀避免冲突
- [ ] 子应用可独立运行（`pnpm dev:sub-xxx`）

## 7.8 AI 操作审批标记检查

- [ ] 保存/提交按钮已添加 `data-ai-approval` 属性
- [ ] 删除按钮已添加 `data-ai-approval` 属性（或使用 `<Popconfirm>` 自动检测）
- [ ] 编辑/修改按钮已添加 `data-ai-approval` 属性
- [ ] 审核/审批操作按钮已添加 `data-ai-approval` 属性
- [ ] 导入/导出按钮已添加 `data-ai-approval` 属性
- [ ] 权限变更操作按钮已添加 `data-ai-approval` 属性
- [ ] 纯查看/导航/搜索按钮**未添加** `data-ai-approval`（避免过度审批）
- [ ] Popconfirm 的确定按钮无需手动添加（自动检测）
- [ ] Modal footer 中使用数组形式定义按钮时，`data-ai-approval` 添加在对应 Button 上

## 7.9 代码质量检查

- [ ] TypeScript 无类型错误
- [ ] 无 `@ts-ignore`（除非有充分理由）
- [ ] 无 `console.log` 残留（错误拦截器中的 `console.error` 除外）
- [ ] 导入顺序符合规范
- [ ] 组件 Props 类型完整
- [ ] `useEffect` 依赖数组正确
- [ ] 事件监听器在 `useEffect` 返回函数中正确清理
