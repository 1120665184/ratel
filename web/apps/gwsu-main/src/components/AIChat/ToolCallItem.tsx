import { LoadingOutlined, CheckCircleOutlined, CloseCircleOutlined, RightOutlined, ThunderboltOutlined, ToolOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import { useState, useMemo } from 'react';
import styles from './ToolCallItem.module.less';


/** ToolCallItem 组件 Props 类型 */
interface ToolCallItemProps {
  name: string;
  status: string;
  args?: Record<string, unknown>;
  result?: unknown;
}


/**
 * 技能名称标识
 */
const SKILL_TOOL_NAME = 'load_skill_through_path';

/**
 * 问题工具名称标识
 */
const QUESTION_TOOL_NAME = 'AskUserQuestion';

/**
 * 工具调用展示组件
 * 在聊天面板中展示工具/技能调用信息，可折叠查看详情
 */
export function ToolCallItem(props: ToolCallItemProps) {
  const { name, status, args, result } = props;
  const [expanded, setExpanded] = useState(false);

  // 判断是否为技能
  const isSkill = name === SKILL_TOOL_NAME;

  // 判断是否为问题
  const isQuestion = name === QUESTION_TOOL_NAME;

  // 解析参数键值
  const argEntries = useMemo(() => Object.entries(args ?? {}), [args]);

  // 解析结果文本
  const resultText =
    typeof result === 'string' ? result : result ? JSON.stringify(result) : '';

  // 解析问题的用户回答
  const questionAnswer = useMemo(() => {
    if (!isQuestion || !resultText) return null;
    try {
      const parsed = JSON.parse(resultText);
      if (parsed.answers) return parsed;
    } catch {
      // result 非 JSON 格式
    }
    return null;
  }, [isQuestion, resultText]);

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
  const skillPath = isSkill ? (args?.path as string) ?? '' : '';

  // 安全获取 questions 数组，格式异常时返回空数组以跳过渲染
  const questionsList: Record<string, unknown>[] = useMemo(() => {
    if (!isQuestion || !Array.isArray(args?.questions)) return [];
    return args.questions as Record<string, unknown>[];
  }, [isQuestion, args?.questions]);

  // 问题展示名：取第一个问题的 header
  const questionHeader = isQuestion ? (questionsList[0]?.header as string) ?? '问题' : '';

  const displayName = isSkill ? skillId : isQuestion ? questionHeader : name;
  const skillDisplayContent = skillPath || skillId || name;

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
    ) : isQuestion ? (
      <QuestionCircleOutlined className={styles.typeIcon} />
    ) : (
      <ToolOutlined className={styles.typeIcon} />
    );

  // 类型标签
  const typeLabel = isSkill ? '技能' : isQuestion ? '问题' : '工具';

  return (
    <div
      className={`${styles.toolCallItem} ${
        isSkill ? styles.skillItem : isQuestion ? styles.questionItem : styles.toolItem
      } ${expanded ? styles.expanded : ''}`}
    >
      {/* 摘要行：箭头 + 类型图标 + 状态 + 名称 */}
      <div className={styles.summaryRow} onClick={() => setExpanded(!expanded)}>
        {renderArrow()}
        {renderTypeIcon()}
        {renderStatusDot()}
        <span className={styles.label}>{typeLabel}</span>
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
              <div className={styles.detailLabel}>路径</div>
              <div className={styles.detailValue}>{skillDisplayContent}</div>
            </div>
          )}

          {/* 问题：展示每个问题和用户回答 */}
          {isQuestion && questionsList.length > 0 && (
            <>
              {questionsList.map((q: Record<string, unknown>, idx: number) => {
                const questionText = q.question as string;
                const options = q.options as Record<string, string>[];
                const answer = questionAnswer?.answers?.[questionText];
                const annotation = questionAnswer?.annotations?.[questionText];
                return (
                  <div key={idx} className={styles.detailSection}>
                    <div className={styles.detailLabel}>问题 {questionsList.length > 1 ? `${idx + 1}` : ''}</div>
                    <div className={styles.questionText}>{questionText}</div>
                    {Array.isArray(options) && options.length > 0 && (
                      <div className={styles.questionOptions}>
                        {options.map((opt, optIdx) => (
                          <span key={optIdx} className={styles.questionOption}>
                            {opt.label}
                          </span>
                        ))}
                      </div>
                    )}
                    {answer && (
                      <div className={styles.questionAnswer}>
                        <span className={styles.questionAnswerLabel}>回答：</span>
                        <span>{answer}</span>
                        {annotation?.notes && annotation.notes !== answer && (
                          <span className={styles.questionAnswerNotes}>（{annotation.notes}）</span>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </>
          )}

          {/* 工具：展示输入 */}
          {!isSkill && !isQuestion && argEntries.length > 0 && (
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
          {!isSkill && !isQuestion && status === 'complete' && resultText && (
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
