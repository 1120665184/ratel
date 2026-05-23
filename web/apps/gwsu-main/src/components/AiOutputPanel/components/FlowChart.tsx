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

function buildFlowOption(props: FlowChartProps): EChartsOption {
  const isVertical = props.direction !== 'horizontal';

  const nodePositions: Record<string, [number, number]> = {};
  let currentY = 0;
  props.nodes.forEach((node) => {
    nodePositions[node.id] = isVertical
      ? [0, currentY]
      : [currentY, 0];
    currentY += isVertical ? 100 : 150;
  });

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
        };
      }),
      links: props.edges.map((edge) => ({
        source: edge.source,
        target: edge.target,
        value: edge.label || '',
        lineStyle: { curveness: 0 },
      })),
    }],
  };
}

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
