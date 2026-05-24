# OutputView 智能体实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现智能助手以精美可视化界面输出内容的功能，后端 OutputViewAgent 输出 json-render spec，前端流式渲染。

**Architecture:** 后端 OutputViewAgent 通过 SkillBox 加载组件技能，LLM 输出 json-render spec（JSONL patch），通过 AG-UI AGENT_OUTPUT 自定义事件流式传输到前端。前端 AiOutputPanel 用 createSpecStreamCompiler 渐进渲染，Renderer 渲染 7 个主题化组件。

**Tech Stack:** Java 25 / Spring Boot 4.0.3 / AgentScope（后端），React 19 / TypeScript / @json-render/core + @json-render/react / zod v4 / echarts / Ant Design 6（前端）

---

## 文件结构

### 后端新增

| 文件 | 职责 |
|------|------|
| `resources/output-view/skill.md` | Skill 总目录：组件一览 + spec 格式规范 + 使用规则 |
| `resources/output-view/components/dashboard.md` | Dashboard 组件详细文档 |
| `resources/output-view/components/section.md` | Section 组件详细文档 |
| `resources/output-view/components/stat-card.md` | StatCard 组件详细文档 |
| `resources/output-view/components/chart.md` | Chart 组件详细文档 |
| `resources/output-view/components/data-table.md` | DataTable 组件详细文档 |
| `resources/output-view/components/text-block.md` | TextBlock 组件详细文档 |
| `resources/output-view/components/flow-chart.md` | FlowChart 组件详细文档 |
| `resources/output-view/specs/examples.md` | 完整示例 |

### 后端修改

| 文件 | 职责 |
|------|------|
| `OutputViewAgent.java` | 完善 build()：注册 SkillBox + sysPrompt 引用 catalog prompt |

### 前端新增

| 文件 | 职责 |
|------|------|
| `services/agent-output/index.ts` | AgentOutput pub/sub 服务 |
| `services/agent-output/types.ts` | AgentOutput 类型定义 |
| `AiOutputPanel/catalog.ts` | json-render catalog 定义（7 组件 Zod schema） |
| `AiOutputPanel/registry.tsx` | json-render registry 定义（7 组件 React 实现） |
| `AiOutputPanel/components/Dashboard.tsx` | Dashboard 容器组件 |
| `AiOutputPanel/components/Section.tsx` | Section 分组容器组件 |
| `AiOutputPanel/components/StatCard.tsx` | StatCard 统计指标卡片组件 |
| `AiOutputPanel/components/Chart.tsx` | Chart 图表组件（ECharts） |
| `AiOutputPanel/components/DataTable.tsx` | DataTable 数据表格组件 |
| `AiOutputPanel/components/TextBlock.tsx` | TextBlock 文本/提示组件 |
| `AiOutputPanel/components/FlowChart.tsx` | FlowChart 流程图组件（ECharts） |
| `AiOutputPanel/components/Dashboard.module.less` | Dashboard + Section 样式 |
| `AiOutputPanel/components/StatCard.module.less` | StatCard 样式 |
| `AiOutputPanel/components/Chart.module.less` | Chart + FlowChart 样式 |
| `AiOutputPanel/components/DataTable.module.less` | DataTable 样式 |
| `AiOutputPanel/components/TextBlock.module.less` | TextBlock 样式 |

### 前端修改

| 文件 | 职责 |
|------|------|
| `AiOutputPanel/index.tsx` | 重写：去掉 iframe，用 json-render Renderer + 流式编译器 |
| `AiOutputPanel/index.module.less` | 重写样式 |
| `CopilotKitProvider.tsx` | 新增 AGENT_OUTPUT 事件处理 |
| `package.json` | 新增 echarts 依赖 |

---

## Task 1: 前端 AgentOutput 服务层

**Files:**
- Create: `web/apps/gwsu-main/src/services/agent-output/types.ts`
- Create: `web/apps/gwsu-main/src/services/agent-output/index.ts`

- [ ] **Step 1: 创建类型定义**

```typescript
// web/apps/gwsu-main/src/services/agent-output/types.ts

/**
 * AGENT_OUTPUT 自定义事件 payload
 * 后端 OutputViewEventHandlerHook 发送的 JSONL patch 数据
 */
export interface AgentOutputPayload {
  /** JSONL patch 字符串或完整 spec JSON */
  text: string;
}
```

- [ ] **Step 2: 创建 pub/sub 服务**

```typescript
// web/apps/gwsu-main/src/services/agent-output/index.ts

export type { AgentOutputPayload } from './types';

import type { AgentOutputPayload } from './types';

/** 输出事件监听器列表 */
const outputListeners = new Set<(payload: AgentOutputPayload) => void>();

/** 当前展示的 spec 内容 */
let currentSpec: string | null = null;

/**
 * 分发 AI 输出事件
 * 由 CopilotKitProvider 中的 CUSTOM 事件监听调用
 */
export function dispatchAgentOutput(payload: AgentOutputPayload): void {
  currentSpec = payload.text;
  outputListeners.forEach((listener) => listener(payload));
}

/**
 * 清除当前输出内容
 */
export function clearAgentOutput(): void {
  currentSpec = null;
  outputListeners.forEach((listener) => listener({ text: '' }));
}

/**
 * 获取当前输出内容
 */
export function getCurrentOutput(): string | null {
  return currentSpec;
}

/**
 * 注册输出事件监听器（供 AiOutputPanel 使用）
 * @returns 取消监听的函数
 */
export function onAgentOutput(listener: (payload: AgentOutputPayload) => void): () => void {
  outputListeners.add(listener);
  return () => {
    outputListeners.delete(listener);
  };
}
```

- [ ] **Step 3: 提交**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add web/apps/gwsu-main/src/services/agent-output/
git commit -m "feat(output-view): 添加前端 agent-output pub/sub 服务层"
```

---

## Task 2: 安装 echarts 依赖

**Files:**
- Modify: `web/apps/gwsu-main/package.json`

- [ ] **Step 1: 安装 echarts**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic/web
pnpm add echarts --filter gwsu-main
```

- [ ] **Step 2: 验证安装成功**

```bash
ls apps/gwsu-main/node_modules/echarts/package.json && echo "OK"
```

Expected: 输出文件路径

- [ ] **Step 3: 提交**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add web/apps/gwsu-main/package.json web/pnpm-lock.yaml
git commit -m "feat(output-view): 安装 echarts 依赖"
```

---

## Task 3: 前端 Catalog 定义

**Files:**
- Create: `web/apps/gwsu-main/src/components/AiOutputPanel/catalog.ts`

- [ ] **Step 1: 创建 catalog**

```typescript
// web/apps/gwsu-main/src/components/AiOutputPanel/catalog.ts

import { defineCatalog } from '@json-render/core';
import { schema } from '@json-render/react/schema';
import { z } from 'zod';

/**
 * 视图输出 Catalog
 * 定义 AI 可输出的 7 种组件
 * catalog.prompt() 生成的提示词将用于后端 OutputViewAgent 的系统提示
 */
export const catalog = defineCatalog(schema, {
  components: {
    Dashboard: {
      props: z.object({
        title: z.string().describe('仪表盘标题'),
        description: z.string().nullable().describe('仪表盘描述'),
      }),
      slots: ['default'],
      description: '根容器组件，定义仪表盘标题和整体布局，必须作为最外层元素',
    },
    Section: {
      props: z.object({
        title: z.string().nullable().describe('分组标题'),
        description: z.string().nullable().describe('分组描述'),
        layout: z.enum(['row', 'column']).nullable().describe('子元素排列方式：row 水平排列，column 垂直排列（默认）'),
      }),
      slots: ['default'],
      description: '分组容器组件，支持行列布局。用 Section 将内容按主题分组，layout="row" 时子元素水平排列（适合并排展示 StatCard），layout="column" 时垂直排列',
    },
    StatCard: {
      props: z.object({
        title: z.string().describe('指标名称，如"总事件数"'),
        value: z.string().describe('指标值，如"1,284"'),
        trend: z.enum(['up', 'down', 'flat']).nullable().describe('趋势方向：up 上升、down 下降、flat 持平'),
        changeRate: z.string().nullable().describe('变化率，如"+12.5%"'),
        icon: z.string().nullable().describe('图标名称（可选）'),
      }),
      description: '统计指标卡片组件，展示单个核心指标及其趋势。通常在 Section(layout="row") 中并排展示多个',
    },
    Chart: {
      props: z.object({
        chartType: z.enum(['bar', 'line', 'pie', 'area']).describe('图表类型：bar 柱状图、line 折线图、pie 饼图、area 面积图'),
        title: z.string().nullable().describe('图表标题'),
        data: z.object({
          categories: z.array(z.string()).describe('X 轴分类标签，如月份名称'),
          series: z.array(z.object({
            name: z.string().describe('系列名称'),
            values: z.array(z.number()).describe('系列数据值'),
          })).describe('数据系列'),
        }).describe('图表数据，categories 为 X 轴标签，series 为数据系列。饼图时 categories 为扇区标签，series 只有一个系列'),
      }),
      description: '图表组件，支持柱状图、折线图、饼图、面积图。数据格式为 categories + series。饼图时 categories 作为扇区标签',
    },
    DataTable: {
      props: z.object({
        title: z.string().nullable().describe('表格标题'),
        columns: z.array(z.object({
          key: z.string().describe('列字段名'),
          label: z.string().describe('列标题'),
          width: z.string().nullable().describe('列宽度，如"120px"'),
        })).describe('列定义'),
        data: z.array(z.record(z.string())).describe('行数据数组，每行是一个 key-value 对象'),
        bordered: z.boolean().nullable().describe('是否显示边框（默认 true）'),
        striped: z.boolean().nullable().describe('是否显示斑马纹（默认 true）'),
      }),
      description: '数据列表组件，展示结构化表格数据。columns 定义列，data 为行数据数组',
    },
    TextBlock: {
      props: z.object({
        content: z.string().describe('文本内容'),
        variant: z.enum(['plain', 'heading', 'info', 'warning', 'error']).nullable().describe('文本变体：plain 普通文本、heading 标题、info 提示、warning 警告、error 错误'),
      }),
      description: '文本/提示/说明组件。heading 用于小标题，plain 用于正文，info/warning/error 用于带背景色的提示信息',
    },
    FlowChart: {
      props: z.object({
        title: z.string().nullable().describe('流程图标题'),
        direction: z.enum(['vertical', 'horizontal']).nullable().describe('布局方向：vertical 垂直（默认）、horizontal 水平'),
        nodes: z.array(z.object({
          id: z.string().describe('节点唯一标识'),
          label: z.string().describe('节点显示文本'),
          type: z.enum(['start', 'process', 'decision', 'end']).describe('节点类型：start 开始、process 处理、decision 判断、end 结束'),
        })).describe('流程节点列表'),
        edges: z.array(z.object({
          source: z.string().describe('起始节点 id'),
          target: z.string().describe('目标节点 id'),
          label: z.string().nullable().describe('边标签，如判断分支的"是"/"否"'),
        })).describe('节点连线列表'),
      }),
      description: '流程图组件，展示流程和决策路径。nodes 定义节点（start/process/decision/end），edges 定义节点间的连线',
    },
  },
});
```

- [ ] **Step 2: 提交**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add web/apps/gwsu-main/src/components/AiOutputPanel/catalog.ts
git commit -m "feat(output-view): 添加 json-render catalog 定义（7组件）"
```

---

## Task 4: 前端组件实现 — 容器组件（Dashboard + Section）

**Files:**
- Create: `web/apps/gwsu-main/src/components/AiOutputPanel/components/Dashboard.tsx`
- Create: `web/apps/gwsu-main/src/components/AiOutputPanel/components/Section.tsx`
- Create: `web/apps/gwsu-main/src/components/AiOutputPanel/components/Dashboard.module.less`

- [ ] **Step 1: 创建 Dashboard + Section 样式**

```less
/* web/apps/gwsu-main/src/components/AiOutputPanel/components/Dashboard.module.less */

.dashboard {
  padding: 20px;
  min-height: 100%;
  background: var(--background-color, #f6f8fc);
}

.title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-color, #1f2937);
  margin: 0 0 4px;
}

.description {
  font-size: 14px;
  color: var(--text-secondary-color, #6b7280);
  margin: 0 0 20px;
  line-height: 1.6;
}

.section {
  margin-bottom: 20px;
}

.sectionTitle {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-color, #1f2937);
  margin: 0 0 4px;
}

.sectionDesc {
  font-size: 13px;
  color: var(--text-secondary-color, #6b7280);
  margin: 0 0 12px;
}

.sectionRow {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;

  & > * {
    flex: 1;
    min-width: 160px;
  }
}

.sectionColumn {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
```

- [ ] **Step 2: 创建 Dashboard 组件**

```tsx
// web/apps/gwsu-main/src/components/AiOutputPanel/components/Dashboard.tsx

import type { BaseComponentProps } from '@json-render/react';
import styles from './Dashboard.module.less';

interface DashboardProps {
  title: string;
  description?: string | null;
}

/**
 * Dashboard 根容器组件
 * 定义仪表盘标题和整体布局
 */
const Dashboard: React.FC<BaseComponentProps<DashboardProps>> = ({ props, children }) => {
  return (
    <div className={styles.dashboard}>
      {props.title && <h1 className={styles.title}>{props.title}</h1>}
      {props.description && <p className={styles.description}>{props.description}</p>}
      {children}
    </div>
  );
};

export default Dashboard;
```

- [ ] **Step 3: 创建 Section 组件**

```tsx
// web/apps/gwsu-main/src/components/AiOutputPanel/components/Section.tsx

import type { BaseComponentProps } from '@json-render/react';
import styles from './Dashboard.module.less';

interface SectionProps {
  title?: string | null;
  description?: string | null;
  layout?: 'row' | 'column' | null;
}

/**
 * Section 分组容器组件
 * 支持行列布局，用 Section 将内容按主题分组
 */
const Section: React.FC<BaseComponentProps<SectionProps>> = ({ props, children }) => {
  const isRow = props.layout === 'row';

  return (
    <div className={styles.section}>
      {props.title && <h2 className={styles.sectionTitle}>{props.title}</h2>}
      {props.description && <p className={styles.sectionDesc}>{props.description}</p>}
      <div className={isRow ? styles.sectionRow : styles.sectionColumn}>
        {children}
      </div>
    </div>
  );
};

export default Section;
```

- [ ] **Step 4: 提交**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add web/apps/gwsu-main/src/components/AiOutputPanel/components/Dashboard.tsx \
       web/apps/gwsu-main/src/components/AiOutputPanel/components/Section.tsx \
       web/apps/gwsu-main/src/components/AiOutputPanel/components/Dashboard.module.less
git commit -m "feat(output-view): 添加 Dashboard + Section 容器组件"
```

---

## Task 5: 前端组件实现 — StatCard

**Files:**
- Create: `web/apps/gwsu-main/src/components/AiOutputPanel/components/StatCard.tsx`
- Create: `web/apps/gwsu-main/src/components/AiOutputPanel/components/StatCard.module.less`

- [ ] **Step 1: 创建 StatCard 样式**

```less
/* web/apps/gwsu-main/src/components/AiOutputPanel/components/StatCard.module.less */

.statCard {
  background: var(--surface-color, #ffffff);
  border-radius: 8px;
  padding: 16px;
  border: 1px solid var(--border-color, #e5e7eb);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

.title {
  font-size: 13px;
  color: var(--text-secondary-color, #6b7280);
  margin: 0 0 6px;
}

.valueRow {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-color, #1f2937);
  margin: 0;
  line-height: 1.2;
}

.trend {
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 2px;
}

.trendUp {
  color: #16a34a;
}

.trendDown {
  color: #dc2626;
}

.trendFlat {
  color: var(--text-secondary-color, #6b7280);
}
```

- [ ] **Step 2: 创建 StatCard 组件**

```tsx
// web/apps/gwsu-main/src/components/AiOutputPanel/components/StatCard.tsx

import { ArrowUpOutlined, ArrowDownOutlined, MinusOutlined } from '@ant-design/icons';
import type { BaseComponentProps } from '@json-render/react';
import styles from './StatCard.module.less';

interface StatCardProps {
  title: string;
  value: string;
  trend?: 'up' | 'down' | 'flat' | null;
  changeRate?: string | null;
  icon?: string | null;
}

const trendConfig = {
  up: { className: styles.trendUp, icon: <ArrowUpOutlined /> },
  down: { className: styles.trendDown, icon: <ArrowDownOutlined /> },
  flat: { className: styles.trendFlat, icon: <MinusOutlined /> },
};

/**
 * StatCard 统计指标卡片组件
 * 展示单个核心指标及其趋势
 */
const StatCard: React.FC<BaseComponentProps<StatCardProps>> = ({ props }) => {
  const trend = props.trend || 'flat';
  const config = trendConfig[trend];

  return (
    <div className={styles.statCard}>
      <div className={styles.title}>{props.title}</div>
      <div className={styles.valueRow}>
        <span className={styles.value}>{props.value}</span>
        {props.changeRate && (
          <span className={`${styles.trend} ${config.className}`}>
            {config.icon}
            {props.changeRate}
          </span>
        )}
      </div>
    </div>
  );
};

export default StatCard;
```

- [ ] **Step 3: 提交**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add web/apps/gwsu-main/src/components/AiOutputPanel/components/StatCard.tsx \
       web/apps/gwsu-main/src/components/AiOutputPanel/components/StatCard.module.less
git commit -m "feat(output-view): 添加 StatCard 统计指标卡片组件"
```

---

## Task 6: 前端组件实现 — Chart（ECharts）

**Files:**
- Create: `web/apps/gwsu-main/src/components/AiOutputPanel/components/Chart.tsx`
- Create: `web/apps/gwsu-main/src/components/AiOutputPanel/components/Chart.module.less`

- [ ] **Step 1: 创建 Chart 样式**

```less
/* web/apps/gwsu-main/src/components/AiOutputPanel/components/Chart.module.less */

.chartContainer {
  background: var(--surface-color, #ffffff);
  border-radius: 8px;
  padding: 16px;
  border: 1px solid var(--border-color, #e5e7eb);
}

.chartTitle {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-color, #1f2937);
  margin: 0 0 12px;
}

.chartWrapper {
  width: 100%;
  height: 300px;
}
```

- [ ] **Step 2: 创建 Chart 组件**

```tsx
// web/apps/gwsu-main/src/components/AiOutputPanel/components/Chart.tsx

import { useRef, useEffect } from 'react';
import * as echarts from 'echarts/core';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { BaseComponentProps } from '@json-render/react';
import type { EChartsOption } from 'echarts';
import styles from './Chart.module.less';

// 注册 ECharts 必要模块
echarts.use([
  BarChart, LineChart, PieChart,
  TitleComponent, TooltipComponent, LegendComponent, GridComponent,
  CanvasRenderer,
]);

interface ChartData {
  categories: string[];
  series: { name: string; values: number[] }[];
}

interface ChartProps {
  chartType: 'bar' | 'line' | 'pie' | 'area';
  title?: string | null;
  data: ChartData;
}

/**
 * 将通用数据格式转换为 ECharts option
 */
function buildOption(props: ChartProps): EChartsOption {
  const { chartType, data } = props;
  const primaryColor = getComputedStyle(document.documentElement)
    .getPropertyValue('--primary-color')
    .trim() || '#1a5fb4';

  const colorPalette = [
    primaryColor,
    '#16a34a', '#d97706', '#6b4c9a', '#dc2626', '#0891b2',
  ];

  if (chartType === 'pie') {
    return {
      color: colorPalette,
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { color: '#6b7280' } },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, color: '#374151' },
        data: data.categories.map((cat, i) => ({
          name: cat,
          value: data.series[0]?.values[i] ?? 0,
        })),
      }],
    };
  }

  // bar / line / area
  const isArea = chartType === 'area';
  return {
    color: colorPalette,
    tooltip: { trigger: 'axis' },
    legend: { data: data.series.map((s) => s.name), textStyle: { color: '#6b7280' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: data.categories, axisLabel: { color: '#6b7280' } },
    yAxis: { type: 'value', axisLabel: { color: '#6b7280' }, splitLine: { lineStyle: { color: '#f3f4f6' } } },
    series: data.series.map((s) => ({
      name: s.name,
      type: chartType === 'area' ? 'line' : chartType,
      data: s.values,
      smooth: true,
      ...(isArea ? { areaStyle: { opacity: 0.15 } } : {}),
      ...(chartType === 'bar' ? { itemStyle: { borderRadius: [4, 4, 0, 0] } } : {}),
    })),
  };
}

/**
 * Chart 图表组件
 * 使用 ECharts 渲染柱状图、折线图、饼图、面积图
 */
const Chart: React.FC<BaseComponentProps<ChartProps>> = ({ props }) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const instanceRef = useRef<echarts.ECharts | null>(null);

  useEffect(() => {
    if (!chartRef.current) return;

    const instance = instanceRef.current || echarts.init(chartRef.current);
    instanceRef.current = instance;
    instance.setOption(buildOption(props));

    const handleResize = () => instance.resize();
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [props]);

  // 组件卸载时销毁实例
  useEffect(() => {
    return () => {
      instanceRef.current?.dispose();
      instanceRef.current = null;
    };
  }, []);

  return (
    <div className={styles.chartContainer}>
      {props.title && <div className={styles.chartTitle}>{props.title}</div>}
      <div ref={chartRef} className={styles.chartWrapper} />
    </div>
  );
};

export default Chart;
```

- [ ] **Step 3: 提交**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add web/apps/gwsu-main/src/components/AiOutputPanel/components/Chart.tsx \
       web/apps/gwsu-main/src/components/AiOutputPanel/components/Chart.module.less
git commit -m "feat(output-view): 添加 Chart 图表组件（ECharts）"
```

---

## Task 7: 前端组件实现 — DataTable

**Files:**
- Create: `web/apps/gwsu-main/src/components/AiOutputPanel/components/DataTable.tsx`
- Create: `web/apps/gwsu-main/src/components/AiOutputPanel/components/DataTable.module.less`

- [ ] **Step 1: 创建 DataTable 样式**

```less
/* web/apps/gwsu-main/src/components/AiOutputPanel/components/DataTable.module.less */

.tableContainer {
  background: var(--surface-color, #ffffff);
  border-radius: 8px;
  padding: 16px;
  border: 1px solid var(--border-color, #e5e7eb);
  overflow-x: auto;
}

.tableTitle {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-color, #1f2937);
  margin: 0 0 12px;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.th {
  padding: 10px 12px;
  text-align: left;
  font-weight: 600;
  color: var(--text-secondary-color, #6b7280);
  background: var(--background-color, #f6f8fc);
  border-bottom: 2px solid var(--border-color, #e5e7eb);
  white-space: nowrap;
}

.td {
  padding: 10px 12px;
  color: var(--text-color, #1f2937);
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.stripedRow {
  background: var(--background-color, #f6f8fc);
}

.borderedTd {
  border: 1px solid var(--border-color, #e5e7eb);
}
```

- [ ] **Step 2: 创建 DataTable 组件**

```tsx
// web/apps/gwsu-main/src/components/AiOutputPanel/components/DataTable.tsx

import type { BaseComponentProps } from '@json-render/react';
import styles from './DataTable.module.less';

interface ColumnDef {
  key: string;
  label: string;
  width?: string | null;
}

interface DataTableProps {
  title?: string | null;
  columns: ColumnDef[];
  data: Record<string, string | number | boolean | null>[];
  bordered?: boolean | null;
  striped?: boolean | null;
}

/**
 * DataTable 数据列表组件
 * 展示结构化表格数据
 */
const DataTable: React.FC<BaseComponentProps<DataTableProps>> = ({ props }) => {
  const isBordered = props.bordered !== false;
  const isStriped = props.striped !== false;

  return (
    <div className={styles.tableContainer}>
      {props.title && <div className={styles.tableTitle}>{props.title}</div>}
      <table className={styles.table}>
        <thead>
          <tr>
            {props.columns.map((col) => (
              <th
                key={col.key}
                className={`${styles.th} ${isBordered ? styles.borderedTd : ''}`}
                style={col.width ? { width: col.width } : undefined}
              >
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {props.data.map((row, rowIdx) => (
            <tr
              key={rowIdx}
              className={isStriped && rowIdx % 2 === 1 ? styles.stripedRow : ''}
            >
              {props.columns.map((col) => (
                <td
                  key={col.key}
                  className={`${styles.td} ${isBordered ? styles.borderedTd : ''}`}
                >
                  {String(row[col.key] ?? '')}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default DataTable;
```

- [ ] **Step 3: 提交**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add web/apps/gwsu-main/src/components/AiOutputPanel/components/DataTable.tsx \
       web/apps/gwsu-main/src/components/AiOutputPanel/components/DataTable.module.less
git commit -m "feat(output-view): 添加 DataTable 数据列表组件"
```

---

## Task 8: 前端组件实现 — TextBlock

**Files:**
- Create: `web/apps/gwsu-main/src/components/AiOutputPanel/components/TextBlock.tsx`
- Create: `web/apps/gwsu-main/src/components/AiOutputPanel/components/TextBlock.module.less`

- [ ] **Step 1: 创建 TextBlock 样式**

```less
/* web/apps/gwsu-main/src/components/AiOutputPanel/components/TextBlock.module.less */

.textBlock {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-color, #1f2937);
  padding: 12px 0;
}

.heading {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-color, #1f2937);
  border-left: 3px solid var(--primary-color, #1a5fb4);
  padding: 8px 12px;
  margin: 0;
  background: var(--surface-color, #ffffff);
  border-radius: 0 6px 6px 0;
}

.info {
  background: #eff6ff;
  color: #1d4ed8;
  border-left: 3px solid #3b82f6;
  padding: 10px 12px;
  border-radius: 0 6px 6px 0;
}

.warning {
  background: #fffbeb;
  color: #92400e;
  border-left: 3px solid #f59e0b;
  padding: 10px 12px;
  border-radius: 0 6px 6px 0;
}

.error {
  background: #fef2f2;
  color: #991b1b;
  border-left: 3px solid #ef4444;
  padding: 10px 12px;
  border-radius: 0 6px 6px 0;
}
```

- [ ] **Step 2: 创建 TextBlock 组件**

```tsx
// web/apps/gwsu-main/src/components/AiOutputPanel/components/TextBlock.tsx

import type { BaseComponentProps } from '@json-render/react';
import styles from './TextBlock.module.less';

interface TextBlockProps {
  content: string;
  variant?: 'plain' | 'heading' | 'info' | 'warning' | 'error' | null;
}

const variantClass: Record<string, string> = {
  heading: styles.heading,
  info: styles.info,
  warning: styles.warning,
  error: styles.error,
};

/**
 * TextBlock 文本/提示/说明组件
 * 支持 5 种变体：plain 普通文本、heading 标题、info 提示、warning 警告、error 错误
 */
const TextBlock: React.FC<BaseComponentProps<TextBlockProps>> = ({ props }) => {
  const variant = props.variant || 'plain';
  const className = variantClass[variant] || styles.textBlock;

  if (variant === 'heading') {
    return <h3 className={className}>{props.content}</h3>;
  }

  return <div className={`${styles.textBlock} ${className}`}>{props.content}</div>;
};

export default TextBlock;
```

- [ ] **Step 3: 提交**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add web/apps/gwsu-main/src/components/AiOutputPanel/components/TextBlock.tsx \
       web/apps/gwsu-main/src/components/AiOutputPanel/components/TextBlock.module.less
git commit -m "feat(output-view): 添加 TextBlock 文本/提示组件"
```

---

## Task 9: 前端组件实现 — FlowChart（ECharts）

**Files:**
- Create: `web/apps/gwsu-main/src/components/AiOutputPanel/components/FlowChart.tsx`

- [ ] **Step 1: 创建 FlowChart 组件**

```tsx
// web/apps/gwsu-main/src/components/AiOutputPanel/components/FlowChart.tsx

import { useRef, useEffect } from 'react';
import * as echarts from 'echarts/core';
import { GraphChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { BaseComponentProps } from '@json-render/react';
import type { EChartsOption } from 'echarts';
import chartStyles from './Chart.module.less';

// 注册 ECharts Graph 模块
echarts.use([GraphChart, TitleComponent, TooltipComponent, CanvasRenderer]);

interface FlowNode {
  id: string;
  label: string;
  type: 'start' | 'process' | 'decision' | 'end';
}

interface FlowEdge {
  source: string;
  target: string;
  label?: string | null;
}

interface FlowChartProps {
  title?: string | null;
  direction?: 'vertical' | 'horizontal' | null;
  nodes: FlowNode[];
  edges: FlowEdge[];
}

/** 节点类型对应的样式 */
const nodeStyles: Record<string, { symbol: string; color: string; borderColor: string }> = {
  start: { symbol: 'circle', color: '#16a34a', borderColor: '#16a34a' },
  process: { symbol: 'roundRect', color: '#ffffff', borderColor: '#1a5fb4' },
  decision: { symbol: 'diamond', color: '#fffbeb', borderColor: '#f59e0b' },
  end: { symbol: 'circle', color: '#dc2626', borderColor: '#dc2626' },
};

/**
 * 将流程图数据转换为 ECharts Graph option
 */
function buildFlowOption(props: FlowChartProps): EChartsOption {
  const isVertical = props.direction !== 'horizontal';

  // 计算节点布局位置
  const nodePositions: Record<string, [number, number]> = {};
  let currentY = 0;
  props.nodes.forEach((node, idx) => {
    nodePositions[node.id] = isVertical
      ? [0, currentY]
      : [currentY, 0];
    currentY += isVertical ? 100 : 150;
  });

  const primaryColor = getComputedStyle(document.documentElement)
    .getPropertyValue('--primary-color')
    .trim() || '#1a5fb4';

  return {
    tooltip: {},
    animation: true,
    series: [{
      type: 'graph',
      layout: 'none',
      symbolSize: 40,
      roam: false,
      label: {
        show: true,
        fontSize: 12,
        color: '#374151',
      },
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: [0, 8],
      edgeLabel: {
        show: true,
        fontSize: 11,
        formatter: '{c}',
        color: '#6b7280',
      },
      lineStyle: {
        color: '#9ca3af',
        width: 1.5,
        curveness: 0,
      },
      data: props.nodes.map((node) => {
        const style = nodeStyles[node.type] || nodeStyles.process;
        return {
          name: node.id,
          x: nodePositions[node.id][0],
          y: nodePositions[node.id][1],
          label: { formatter: node.label },
          symbol: style.symbol,
          symbolSize: node.type === 'decision' ? [60, 40] : node.type === 'start' || node.type === 'end' ? 50 : [80, 36],
          itemStyle: {
            color: style.color,
            borderColor: style.borderColor,
            borderWidth: 2,
          },
          labelLayout: {
            hideOverlap: false,
          },
        };
      }),
      links: props.edges.map((edge) => ({
        source: edge.source,
        target: edge.target,
        value: edge.label || '',
        lineStyle: {
          curveness: 0,
        },
      })),
    }],
  };
}

/**
 * FlowChart 流程图组件
 * 使用 ECharts Graph 渲染流程图
 */
const FlowChart: React.FC<BaseComponentProps<FlowChartProps>> = ({ props }) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const instanceRef = useRef<echarts.ECharts | null>(null);

  useEffect(() => {
    if (!chartRef.current) return;

    const instance = instanceRef.current || echarts.init(chartRef.current);
    instanceRef.current = instance;
    instance.setOption(buildFlowOption(props));

    const handleResize = () => instance.resize();
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [props]);

  useEffect(() => {
    return () => {
      instanceRef.current?.dispose();
      instanceRef.current = null;
    };
  }, []);

  return (
    <div className={chartStyles.chartContainer}>
      {props.title && <div className={chartStyles.chartTitle}>{props.title}</div>}
      <div ref={chartRef} className={chartStyles.chartWrapper} />
    </div>
  );
};

export default FlowChart;
```

- [ ] **Step 2: 提交**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add web/apps/gwsu-main/src/components/AiOutputPanel/components/FlowChart.tsx
git commit -m "feat(output-view): 添加 FlowChart 流程图组件（ECharts）"
```

---

## Task 10: 前端 Registry 定义

**Files:**
- Create: `web/apps/gwsu-main/src/components/AiOutputPanel/registry.tsx`

- [ ] **Step 1: 创建 registry**

```tsx
// web/apps/gwsu-main/src/components/AiOutputPanel/registry.tsx

import { defineRegistry } from '@json-render/react';
import { catalog } from './catalog';
import Dashboard from './components/Dashboard';
import Section from './components/Section';
import StatCard from './components/StatCard';
import Chart from './components/Chart';
import DataTable from './components/DataTable';
import TextBlock from './components/TextBlock';
import FlowChart from './components/FlowChart';

/**
 * 视图输出组件 Registry
 * 将 catalog 定义的 7 个组件映射到 React 渲染实现
 */
export const { registry } = defineRegistry(catalog, {
  components: {
    Dashboard,
    Section,
    StatCard,
    Chart,
    DataTable,
    TextBlock,
    FlowChart,
  },
});
```

- [ ] **Step 2: 提交**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add web/apps/gwsu-main/src/components/AiOutputPanel/registry.tsx
git commit -m "feat(output-view): 添加 json-render registry 定义"
```

---

## Task 11: 前端 AiOutputPanel 重写

**Files:**
- Modify: `web/apps/gwsu-main/src/components/AiOutputPanel/index.tsx`
- Modify: `web/apps/gwsu-main/src/components/AiOutputPanel/index.module.less`

- [ ] **Step 1: 重写 AiOutputPanel 样式**

```less
/* web/apps/gwsu-main/src/components/AiOutputPanel/index.module.less */

.aiOutputPanel {
  width: 100%;
  height: 100%;
  position: relative;
  overflow-y: auto;
  overflow-x: hidden;
  background: var(--background-color, #f6f8fc);

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--border-color, #d1d5db);
    border-radius: 3px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }
}

.emptyState {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary-color, #6b7280);
  text-align: center;
  padding: 48px 28px;
  background: var(--background-color, #f6f8fc);
}

.emptyIcon {
  font-size: 48px;
  margin-bottom: 20px;
  color: var(--primary-color, #1a5fb4);
  opacity: 0.3;
}

.emptyTitle {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color, #374151);
  margin-bottom: 8px;
}

.emptySubtitle {
  font-size: 14px;
  color: var(--text-secondary-color, #6b7280);
  line-height: 1.6;
}

.loadingIndicator {
  position: absolute;
  bottom: 16px;
  right: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary-color, #6b7280);
  background: var(--surface-color, #ffffff);
  padding: 4px 10px;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.loadingDot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary-color, #1a5fb4);
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1); }
}
```

- [ ] **Step 2: 重写 AiOutputPanel 组件**

```tsx
// web/apps/gwsu-main/src/components/AiOutputPanel/index.tsx

import { RobotOutlined } from '@ant-design/icons';
import { useEffect, useState, useRef, useCallback } from 'react';
import { Renderer } from '@json-render/react';
import { createSpecStreamCompiler } from '@json-render/core';
import type { Spec } from '@json-render/core';
import { registry } from './registry';
import { onAgentOutput, clearAgentOutput } from '@/services/agent-output';
import styles from './index.module.less';

/**
 * AI 输出面板组件
 * 使用 json-render Renderer 流式渲染 AI 输出的可视化内容
 * 组件不会被销毁，切换 Tab 时仅隐藏
 */
const AiOutputPanel: React.FC = () => {
  const [spec, setSpec] = useState<Spec | null>(null);
  const [isStreaming, setIsStreaming] = useState(false);
  const [hasContent, setHasContent] = useState(false);
  const compilerRef = useRef<ReturnType<typeof createSpecStreamCompiler> | null>(null);

  // 确保编译器只创建一次
  if (!compilerRef.current) {
    compilerRef.current = createSpecStreamCompiler();
  }

  const handleClear = useCallback(() => {
    compilerRef.current = createSpecStreamCompiler();
    setSpec(null);
    setHasContent(false);
    setIsStreaming(false);
    clearAgentOutput();
  }, []);

  useEffect(() => {
    const unsubscribe = onAgentOutput(({ text }) => {
      if (!text) {
        // 空文本表示清除
        handleClear();
        return;
      }

      setIsStreaming(true);

      try {
        // 尝试作为完整 spec JSON 解析
        const parsed = JSON.parse(text);
        if (parsed && typeof parsed === 'object' && parsed.root && parsed.elements) {
          compilerRef.current = createSpecStreamCompiler();
          setSpec(parsed);
          setHasContent(true);
          setIsStreaming(false);
          return;
        }
      } catch {
        // 不是完整 JSON，尝试作为 JSONL patch
      }

      // 尝试作为 JSONL patch 行处理
      try {
        const { result } = compilerRef.current.push(text);
        if (result && result.root && Object.keys(result.elements || {}).length > 0) {
          setSpec({ ...result });
          setHasContent(true);
        }
      } catch {
        // patch 解析失败，忽略
      }
    });

    return unsubscribe;
  }, [handleClear]);

  return (
    <div className={styles.aiOutputPanel}>
      {hasContent && spec && (
        <Renderer spec={spec} registry={registry} />
      )}
      {isStreaming && (
        <div className={styles.loadingIndicator}>
          <span className={styles.loadingDot} />
          生成中...
        </div>
      )}
      {!hasContent && !isStreaming && (
        <div className={styles.emptyState}>
          <RobotOutlined className={styles.emptyIcon} />
          <div className={styles.emptyTitle}>AI 输出区</div>
          <div className={styles.emptySubtitle}>智能助手的输出结果将在此展示</div>
        </div>
      )}
    </div>
  );
};

export default AiOutputPanel;
```

- [ ] **Step 3: 提交**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add web/apps/gwsu-main/src/components/AiOutputPanel/index.tsx \
       web/apps/gwsu-main/src/components/AiOutputPanel/index.module.less
git commit -m "feat(output-view): 重写 AiOutputPanel，使用 json-render 流式渲染"
```

---

## Task 12: 前端 CopilotKitProvider 集成 AGENT_OUTPUT

**Files:**
- Modify: `web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx`

- [ ] **Step 1: 添加 AGENT_OUTPUT 事件处理**

在 CopilotKitProvider.tsx 中：
1. 新增 import: `import { dispatchAgentOutput } from '@/services/agent-output';`
2. 新增 import: `import type { AgentOutputPayload } from '@/services/agent-output';`
3. 在 `WebToolEventListener` 的 `onCustomEvent` 回调中，在 `HUMAN_APPROVAL` 分支之后新增：

```typescript
// AI 输出视图
else if (event.name === 'AGENT_OUTPUT') {
  dispatchAgentOutput(event.value as AgentOutputPayload);
}
```

完整的 `onCustomEvent` 回调变为：

```typescript
onCustomEvent: ({ event }): void => {
  // web 工具调用
  if (event.name === 'TOOL_EXECUTE') {
    dispatchWebTool(event.value as WebToolExecutePayload);
  }
  // 人工干预审批
  else if (event.name === 'HUMAN_APPROVAL') {
    dispatchHumanApproval(event.value as HumanApprovalPayload);
  }
  // AI 输出视图
  else if (event.name === 'AGENT_OUTPUT') {
    dispatchAgentOutput(event.value as AgentOutputPayload);
  }
},
```

- [ ] **Step 2: 提交**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx
git commit -m "feat(output-view): CopilotKitProvider 集成 AGENT_OUTPUT 事件处理"
```

---

## Task 13: 后端 skill.md + 组件文档

**Files:**
- Create: `business/business-security/business-security-server/src/main/resources/output-view/skill.md`
- Create: `business/business-security/business-security-server/src/main/resources/output-view/components/dashboard.md`
- Create: `business/business-security/business-security-server/src/main/resources/output-view/components/section.md`
- Create: `business/business-security/business-security-server/src/main/resources/output-view/components/stat-card.md`
- Create: `business/business-security/business-security-server/src/main/resources/output-view/components/chart.md`
- Create: `business/business-security/business-security-server/src/main/resources/output-view/components/data-table.md`
- Create: `business/business-security/business-security-server/src/main/resources/output-view/components/text-block.md`
- Create: `business/business-security/business-security-server/src/main/resources/output-view/components/flow-chart.md`
- Create: `business/business-security/business-security-server/src/main/resources/output-view/specs/examples.md`

- [ ] **Step 1: 创建 skill.md（总目录 + spec 格式规范）**

```markdown
# 视图输出技能

你是一个专门将内容以精美可视化界面展示的智能体。
你输出的内容将以前端组件的形式渲染给用户，必须输出符合以下规范的 JSON。

## Spec 格式规范

你输出的 JSON 必须遵循以下格式：

```json
{
  "root": "元素key",
  "elements": {
    "元素key": {
      "type": "组件名称",
      "props": { 组件属性 },
      "children": ["子元素key1", "子元素key2"]
    }
  }
}
```

### 规则

1. `root` 指向最外层元素，该元素必须是 `Dashboard` 类型
2. `elements` 是扁平映射，每个元素有唯一的 key
3. 每个元素包含 `type`（组件名）、`props`（属性）、`children`（子元素 key 数组）
4. 叶子组件（StatCard、Chart、DataTable、TextBlock、FlowChart）的 `children` 为空数组 `[]`
5. 容器组件（Dashboard、Section）的 `children` 包含子元素的 key
6. 输出纯 JSON，不要包含 markdown 代码块标记

## 支持的组件

| 组件 | 用途 | 详细文档 |
|------|------|----------|
| Dashboard | 根容器，定义标题和整体布局 | @components/dashboard.md |
| Section | 分组容器，支持行列布局 | @components/section.md |
| StatCard | 统计指标卡片（数值+趋势） | @components/stat-card.md |
| Chart | 图表（柱状/折线/饼图/面积） | @components/chart.md |
| DataTable | 数据列表 | @components/data-table.md |
| TextBlock | 文本/提示/说明 | @components/text-block.md |
| FlowChart | 流程图（节点+连线） | @components/flow-chart.md |

## 使用规则

1. 必须以 `Dashboard` 作为根元素
2. 使用 `Section` 对内容进行分组，`layout="row"` 用于并排展示（如多个 StatCard），`layout="column"` 用于垂直排列
3. 统计数据优先使用 `StatCard`，趋势用 `Chart`，明细用 `DataTable`
4. 提示信息使用 `TextBlock`，根据重要程度选择 `info`/`warning`/`error` 变体
5. 流程和决策路径使用 `FlowChart`
6. 完整示例见 @specs/examples.md
```

- [ ] **Step 2: 创建 dashboard.md**

```markdown
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
```

- [ ] **Step 3: 创建 section.md**

```markdown
# Section 组件

分组容器组件，支持行列布局。用于将内容按主题分组。

## Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 否 | 分组标题 |
| description | string | 否 | 分组描述 |
| layout | "row" \| "column" | 否 | 子元素排列方式，默认 column。row 适合并排展示 StatCard，column 适合垂直排列 |

## 示例

```json
{
  "type": "Section",
  "props": { "title": "核心指标", "layout": "row" },
  "children": ["card1", "card2", "card3"]
}
```
```

- [ ] **Step 4: 创建 stat-card.md**

```markdown
# StatCard 组件

统计指标卡片，展示单个核心指标及其趋势变化。通常在 Section(layout="row") 中并排展示多个。

## Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 指标名称，如"总事件数" |
| value | string | 是 | 指标值，如"1,284" |
| trend | "up" \| "down" \| "flat" | 否 | 趋势方向 |
| changeRate | string | 否 | 变化率，如"+12.5%" |
| icon | string | 否 | 图标名称（保留） |

## 示例

```json
{
  "type": "StatCard",
  "props": { "title": "总事件数", "value": "1,284", "trend": "up", "changeRate": "+12.5%" },
  "children": []
}
```
```

- [ ] **Step 5: 创建 chart.md**

```markdown
# Chart 组件

图表组件，支持柱状图、折线图、饼图、面积图。

## Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chartType | "bar" \| "line" \| "pie" \| "area" | 是 | 图表类型 |
| title | string | 否 | 图表标题 |
| data | object | 是 | 图表数据 |

## data 结构

```json
{
  "categories": ["1月", "2月", "3月"],
  "series": [
    { "name": "事件数", "values": [320, 410, 380] }
  ]
}
```

- `categories`: X 轴分类标签数组
- `series`: 数据系列数组，每个系列有 name 和 values
- 饼图时 categories 作为扇区标签，series 只有一个系列

## 示例

```json
{
  "type": "Chart",
  "props": {
    "chartType": "bar",
    "title": "月度事件趋势",
    "data": {
      "categories": ["1月", "2月", "3月"],
      "series": [{ "name": "事件数", "values": [320, 410, 380] }]
    }
  },
  "children": []
}
```
```

- [ ] **Step 6: 创建 data-table.md**

```markdown
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
```

- [ ] **Step 7: 创建 text-block.md**

```markdown
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
```

- [ ] **Step 8: 创建 flow-chart.md**

```markdown
# FlowChart 组件

流程图组件，展示流程和决策路径。

## Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 否 | 流程图标题 |
| direction | "vertical" \| "horizontal" | 否 | 布局方向，默认 vertical |
| nodes | array | 是 | 流程节点列表 |
| edges | array | 是 | 节点连线列表 |

## nodes 结构

```json
[
  { "id": "start", "label": "事件上报", "type": "start" },
  { "id": "review", "label": "安全评审", "type": "process" },
  { "id": "approve", "label": "审批通过", "type": "decision" },
  { "id": "end", "label": "归档完成", "type": "end" }
]
```

节点类型：
- `start`: 开始节点（绿色圆形）
- `process`: 处理节点（蓝色圆角矩形）
- `decision`: 判断节点（黄色菱形）
- `end`: 结束节点（红色圆形）

## edges 结构

```json
[
  { "source": "start", "target": "review" },
  { "source": "approve", "target": "handle", "label": "是" },
  { "source": "approve", "target": "review", "label": "否" }
]
```

## 示例

```json
{
  "type": "FlowChart",
  "props": {
    "title": "安全事件处理流程",
    "direction": "vertical",
    "nodes": [
      { "id": "start", "label": "事件上报", "type": "start" },
      { "id": "review", "label": "安全评审", "type": "process" },
      { "id": "end", "label": "归档完成", "type": "end" }
    ],
    "edges": [
      { "source": "start", "target": "review" },
      { "source": "review", "target": "end" }
    ]
  },
  "children": []
}
```
```

- [ ] **Step 9: 创建 examples.md**

```markdown
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
```

- [ ] **Step 10: 提交**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add business/business-security/business-security-server/src/main/resources/output-view/
git commit -m "feat(output-view): 添加后端 skill 文档和组件资源"
```

---

## Task 14: 后端 OutputViewAgent 完善

**Files:**
- Modify: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/brain/service/agent/OutputViewAgent.java`

- [ ] **Step 1: 完善 OutputViewAgent**

修改 OutputViewAgent.java：
1. 添加 SkillBox 构建
2. 添加资源加载方法
3. 完善 buildSystemPrompt()
4. 使用 catalog.prompt() 生成提示词

```java
package org.quyq.gwsu.security.brain.service.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.subagent.SubAgentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 视图输出智能体
 * 将回复内容以精美的可视化界面展示给用户
 * 输出 json-render spec，通过 AGENT_OUTPUT 自定义事件发送到前端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutputViewAgent {

    public static final String AGENT_NAME = "OutputViewAgent";

    private static final String SKILL_RESOURCE_PATH = "output-view/skill.md";

    private final ObjectProvider<Memory> memoryProvider;

    private final ObjectProvider<Toolkit> toolkitProvider;

    private final Session agentSession;

    private final Model model;

    /**
     * 构建视图输出智能体
     */
    public Agent build() {
        Memory memory = memoryProvider.getIfAvailable();
        Toolkit toolkit = toolkitProvider.getIfAvailable(Toolkit::new);

        // 构建技能盒子
        SkillBox skillBox = buildSkillBox(toolkit);

        return ReActAgent.builder()
                .name(AGENT_NAME)
                .sysPrompt(buildSystemPrompt())
                .memory(memory)
                .model(model)
                .toolkit(toolkit)
                .skillBox(skillBox)
                .build();
    }

    /**
     * 构建技能盒子
     * 注册 output_view 技能，包含组件目录和 spec 格式规范
     */
    private SkillBox buildSkillBox(Toolkit toolkit) {
        SkillBox skillBox = new SkillBox(toolkit);

        String skillContent = loadResource(SKILL_RESOURCE_PATH);

        AgentSkill outputViewSkill = AgentSkill.builder()
                .name("output_view")
                .description("将回复内容以可视化界面展示给用户时加载此技能，包含支持的组件列表和 spec 格式规范")
                .skillContent(skillContent)
                .build();

        skillBox.registration()
                .skill(outputViewSkill)
                .apply();

        return skillBox;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return """
                # 角色
                你是一个专业的数据可视化输出智能体。你的任务是将信息以精美的可视化界面展示给用户。

                # 工作流程
                1. 分析用户需要展示的内容类型
                2. 加载 output_view 技能，了解支持的组件和 spec 格式
                3. 根据内容选择合适的组件，阅读对应的详细文档
                4. 输出符合 spec 格式的 JSON

                # 输出要求
                1. 必须输出纯 JSON 格式，不要包含 markdown 代码块标记
                2. 根元素必须是 Dashboard 类型
                3. 使用 Section 对内容进行分组
                4. 合理使用布局：并排展示用 layout="row"，垂直排列用 layout="column"
                5. 选择最合适的组件类型展示数据

                # 重要提示
                - 你输出的 JSON 会被前端流式渲染，请确保格式正确
                - 不要输出任何 JSON 之外的额外文字说明
                - 如果数据量大，优先用 DataTable；如果需要展示趋势，用 Chart
                """;
    }

    /**
     * 从 classpath 加载资源文件内容
     */
    private String loadResource(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            log.error("加载资源文件失败: {}", path, e);
            return "";
        }
    }

    /**
     * 获取子智能体配置（供其他智能体调用）
     */
    public SubAgentConfig getSubAgentConfig() {
        return SubAgentConfig.builder()
                .toolName(AGENT_NAME)
                .description("""
                        将回复内容以漂亮的UI形式展示给用户。
                        以下场景适合调用：
                        - 数据统计展示
                        - 数据分析报表展示
                        - 流程图展示
                        - 对比分析展示
                        """)
                .session(agentSession)
                .forwardEvents(true)
                .build();
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
mvn compile -pl business/business-security/business-security-server -am -DskipTests -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/brain/service/agent/OutputViewAgent.java
git commit -m "feat(output-view): 完善 OutputViewAgent，注册 SkillBox + 系统提示词"
```

---

## Task 15: 前端构建验证

**Files:** 无新增

- [ ] **Step 1: 构建前端共享库**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic/web
pnpm build:core
```

Expected: 构建成功

- [ ] **Step 2: 构建主应用**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic/web
pnpm build:main
```

Expected: 构建成功，无 TypeScript 错误

- [ ] **Step 3: 如有编译错误，修复后重新构建**

根据错误信息修复，直至构建成功。

- [ ] **Step 4: 提交修复（如有）**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic
git add -A
git commit -m "fix(output-view): 修复前端构建错误"
```
