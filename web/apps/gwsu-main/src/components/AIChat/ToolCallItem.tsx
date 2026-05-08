import { LoadingOutlined, CheckCircleOutlined, CloseCircleOutlined, RightOutlined, ThunderboltOutlined, ToolOutlined } from '@ant-design/icons';
import type { CatchAllActionRenderProps } from '@copilotkit/react-core';
import { useState, useMemo } from 'react';
import styles from './ToolCallItem.module.less';
// @ts-ignore
import { Parameter } from '@copilotkit/shared';


/**
 * 技能名称标识
 */
const SKILL_TOOL_NAME = 'load_skill_through_path';

/**
 * 工具调用展示组件
 * 在聊天面板中展示工具/技能调用信息，可折叠查看详情
 */
export function ToolCallItem(
  props: CatchAllActionRenderProps<Parameter<string, unknown>>,
) {
  const { name, status, args, result } = props;
  const [expanded, setExpanded] = useState(false);

  // 判断是否为技能
  const isSkill = name === SKILL_TOOL_NAME;

  // 解析参数键值
  const argEntries = useMemo(() => Object.entries(args ?? {}), [args]);

  // 解析结果文本
  const resultText =
    typeof result === 'string' ? result : result ? JSON.stringify(result) : '';

  // 判断结果是否有错误
  const hasError =
    status === 'complete' &&
      resultText &&
      resultText.includes('Error:');

  // 根据状态渲染状态点
  const renderStatusDot = () => {
    if (status === 'inProgress' || status === 'executing') {
      return (
        <span className={`${styles.statusDot} ${styles.statusDotExecuting}`}>
          <LoadingOutlined spin />
        </span>
      );
    }
    if (status === 'complete') {
      return hasError ? (
        <span className={`${styles.statusDot} ${styles.statusDotError}`}>
          <CloseCircleOutlined />
        </span>
      ) : (
        <span className={`${styles.statusDot} ${styles.statusDotComplete}`}>
          <CheckCircleOutlined />
        </span>
      );
    }
    return (
      <span className={`${styles.statusDot} ${styles.statusDotPending}`} />
    );
  };

  // 技能展示名
  const skillId = isSkill ? (args?.skillId as string) ?? '' : '';

  const displayName = isSkill ? skillId : name;

  // 折叠箭头
  const renderArrow = () => (
    <span
      className={`${styles.arrow} ${expanded ? styles.arrowExpanded : ''}`}
      onClick={(e) => {
        e.stopPropagation();
        setExpanded(!expanded);
      }}
    >
      <RightOutlined />
    </span>
  );

  // 类型图标
  const renderTypeIcon = () =>
    isSkill ? (
      <ThunderboltOutlined className={styles.typeIcon} />
    ) : (
      <ToolOutlined className={styles.typeIcon} />
    );

  return (
    <div
      className={`${styles.toolCallItem} ${
        isSkill ? styles.skillItem : styles.toolItem
      } ${expanded ? styles.expanded : ''}`}
    >
      {/* 摘要行：箭头 + 类型图标 + 状态 + 名称 */}
      <div className={styles.summaryRow} onClick={() => setExpanded(!expanded)}>
        {renderArrow()}
        {renderTypeIcon()}
        {renderStatusDot()}
        <span className={styles.label}>{isSkill ? '技能' : '工具'}</span>
        <span className={styles.displayName}>{displayName}</span>
      </div>

      {/* 可折叠的详情区域 */}
      <div
        className={`${styles.detailPanel} ${
          expanded ? styles.detailPanelVisible : ''
        }`}
      >
        <div className={styles.detailInner}>
          {/* 技能：仅展示技能名 */}
          {isSkill && (
            <div className={styles.detailSection}>
              <div className={styles.detailLabel}>技能</div>
              <div className={styles.detailValue}>{skillId}</div>
            </div>
          )}

          {/* 工具：展示输入 */}
          {!isSkill && argEntries.length > 0 && (
            <div className={styles.detailSection}>
              <div className={styles.detailLabel}>输入</div>
              <div className={styles.detailContent}>
                {argEntries.map(([key, value]) => (
                  <div key={key} className={styles.kvRow}>
                    <span className={styles.kvKey}>{key}</span>
                    <span className={styles.kvValue}>
                      {typeof value === 'string'
                        ? value
                        : JSON.stringify(value)}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 工具：展示输出 */}
          {!isSkill && status === 'complete' && resultText && (
            <div className={styles.detailSection}>
              <div
                className={`${styles.detailLabel} ${
                  hasError ? styles.detailLabelError : ''
                }`}
              >
                输出
              </div>
              <div
                className={`${styles.detailContent} ${
                  hasError ? styles.detailContentError : ''
                }`}
              >
                {resultText}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
