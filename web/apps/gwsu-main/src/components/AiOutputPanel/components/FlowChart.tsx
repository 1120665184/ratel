import { useRef, useEffect, useMemo } from 'react';
import * as echarts from 'echarts/core';
import { GraphChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { BaseComponentProps } from '@json-render/react';
import type { EChartsOption } from 'echarts';
import styles from './FlowChart.module.less';

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

const nodeStyles: Record<string, { symbol: string; color: string; borderColor: string }> = {
  start: { symbol: 'circle', color: '#16a34a', borderColor: '#16a34a' },
  process: { symbol: 'roundRect', color: '#ffffff', borderColor: '#1a5fb4' },
  decision: { symbol: 'diamond', color: '#fffbeb', borderColor: '#f59e0b' },
  end: { symbol: 'circle', color: '#dc2626', borderColor: '#dc2626' },
};

/** 垂直布局间距 */
const VERTICAL_GAP = 90;
/** 水平布局间距 */
const HORIZONTAL_GAP = 160;
/** 回边的水平偏移量 */
const BACK_EDGE_OFFSET = 200;

/**
 * 计算节点布局位置
 * 正向边沿主轴排列，回边（target 在 source 之前）通过偏移实现
 */
function calculateNodePositions(
  nodes: FlowNode[],
  edges: FlowEdge[],
  direction: 'vertical' | 'horizontal',
): { positions: Record<string, [number, number]>; maxMain: number; maxCross: number } {
  const isVertical = direction === 'vertical';
  const gap = isVertical ? VERTICAL_GAP : HORIZONTAL_GAP;

  // 按输入顺序分配主轴坐标
  const positions: Record<string, [number, number]> = {};
  nodes.forEach((node, i) => {
    const main = i * gap;
    positions[node.id] = isVertical ? [0, main] : [main, 0];
  });

  // 检测回边，为回边涉及的节点添加交叉轴偏移
  const nodeIndexMap = new Map(nodes.map((n, i) => [n.id, i]));
  const backEdgeSources = new Set<string>();

  edges.forEach((edge) => {
    const sourceIdx = nodeIndexMap.get(edge.source);
    const targetIdx = nodeIndexMap.get(edge.target);
    if (sourceIdx !== undefined && targetIdx !== undefined && targetIdx < sourceIdx) {
      // 回边：source 向右偏移
      backEdgeSources.add(edge.source);
    }
  });

  // 有回边时，将回边源节点及之后的节点整体向交叉轴偏移
  if (backEdgeSources.size > 0) {
    let hasBackEdge = false;
    nodes.forEach((node) => {
      if (backEdgeSources.has(node.id)) hasBackEdge = true;
      if (hasBackEdge) {
        const pos = positions[node.id];
        positions[node.id] = isVertical
          ? [pos[0] + BACK_EDGE_OFFSET, pos[1]]
          : [pos[0], pos[1] + BACK_EDGE_OFFSET];
      }
    });
  }

  // 计算边界
  let maxMain = 0;
  let maxCross = 0;
  Object.values(positions).forEach(([x, y]) => {
    if (isVertical) {
      maxMain = Math.max(maxMain, y);
      maxCross = Math.max(maxCross, x);
    } else {
      maxMain = Math.max(maxMain, x);
      maxCross = Math.max(maxCross, y);
    }
  });

  return { positions, maxMain, maxCross };
}

function buildFlowOption(props: FlowChartProps): EChartsOption {
  const direction = props.direction === 'horizontal' ? 'horizontal' : 'vertical';
  const { positions } = calculateNodePositions(props.nodes, props.edges, direction);

  return {
    tooltip: {},
    animation: true,
    series: [{
      type: 'graph',
      layout: 'none',
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
        curveness: 0.2,
      },
      data: props.nodes.map((node) => {
        const style = nodeStyles[node.type] || nodeStyles.process;
        return {
          name: node.id,
          x: positions[node.id][0],
          y: positions[node.id][1],
          label: { formatter: node.label },
          symbol: style.symbol,
          symbolSize: node.type === 'decision' ? [60, 40] : node.type === 'start' || node.type === 'end' ? 50 : [80, 36],
          itemStyle: {
            color: style.color,
            borderColor: style.borderColor,
            borderWidth: 2,
          },
        };
      }),
      links: props.edges.map((edge) => {
        const nodeIndexMap = new Map(props.nodes.map((n, i) => [n.id, i]));
        const sourceIdx = nodeIndexMap.get(edge.source);
        const targetIdx = nodeIndexMap.get(edge.target);
        const isBackEdge = sourceIdx !== undefined && targetIdx !== undefined && targetIdx < sourceIdx;

        return {
          source: edge.source,
          target: edge.target,
          value: edge.label || '',
          lineStyle: {
            curveness: isBackEdge ? 0.3 : 0.1,
          },
        };
      }),
    }],
  };
}

const FlowChart: React.FC<BaseComponentProps<FlowChartProps>> = ({ props }) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const instanceRef = useRef<echarts.ECharts | null>(null);

  const direction = props.direction === 'horizontal' ? 'horizontal' : 'vertical';

  // 根据节点数量动态计算高度
  const { maxMain, maxCross } = useMemo(
    () => calculateNodePositions(props.nodes || [], props.edges || [], direction),
    [props.nodes, props.edges, direction],
  );

  const isVertical = direction === 'vertical';
  const wrapperStyle = isVertical
    ? { height: Math.max(300, maxMain + 100) }
    : { height: Math.max(300, maxCross + 160), minHeight: 300 };

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
    <div className={styles.flowContainer}>
      {props.title && <div className={styles.flowTitle}>{props.title}</div>}
      <div ref={chartRef} className={styles.flowWrapper} style={wrapperStyle} />
    </div>
  );
};

export default FlowChart;
