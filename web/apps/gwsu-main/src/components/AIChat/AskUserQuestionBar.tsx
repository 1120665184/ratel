import  { useState, useEffect, useCallback, useRef } from 'react';
import { Button, Input, Radio, Checkbox } from 'antd';
import { QuestionCircleOutlined } from '@ant-design/icons';
import { useAgent } from '@copilotkit/react-core/v2';
import { onAskUserQuestion, clearAskUserQuestion } from '@/services/ask-user-question';
import type { AskUserQuestionPayload, AskUserQuestionAnswer, QuestionParam } from '@/services/ask-user-question';
import styles from './AskUserQuestionBar.module.less';

/**
 * AskUserQuestion 嵌入式选择框
 * 分步式导航，一次展示一个问题，回答完自动跳转下一个
 * 与 HumanApprovalBar 平级，弹框活跃时输入框禁用
 */
export function AskUserQuestionBar() {
  const [payload, setPayload] = useState<AskUserQuestionPayload | null>(null);
  const [currentStep, setCurrentStep] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [annotations, setAnnotations] = useState<Record<string, { preview?: string; notes?: string }>>({});
  const [otherInputs, setOtherInputs] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const { agent } = useAgent({ agentId: 'brain' });
  const autoAdvanceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // 监听 AskUserQuestion 事件
  useEffect(() => {
    const unsubscribe = onAskUserQuestion((p) => {
      if (autoAdvanceTimer.current) {
        clearTimeout(autoAdvanceTimer.current);
        autoAdvanceTimer.current = null;
      }
      setPayload(p);
      setCurrentStep(0);
      setAnswers({});
      setAnnotations({});
      setOtherInputs({});
    });
    return unsubscribe;
  }, []);

  // 组件卸载时清除定时器
  useEffect(() => {
    return () => {
      if (autoAdvanceTimer.current) {
        clearTimeout(autoAdvanceTimer.current);
      }
    };
  }, []);

  // 提交答案
  const submitAnswer = useCallback(async () => {
    if (!payload) return;
    setSubmitting(true);
    try {
      const answer: AskUserQuestionAnswer = { answers, annotations };
      const toolMsgId = crypto.randomUUID();
      agent.addMessage({
        id: toolMsgId,
        role: 'tool',
        content: JSON.stringify(answer),
        toolCallId: payload.toolCallId,
      } as any);

      clearAskUserQuestion();
      await agent.runAgent();
    } catch (error) {
      console.error('[AskUserQuestion] 提交答案失败:', error);
    } finally {
      setSubmitting(false);
    }
  }, [payload, answers, annotations, agent]);

  // 选中选项
  const handleSelect = useCallback((question: QuestionParam, label: string, isOther: boolean) => {
    const key = question.question;

    if (question.multiSelect) {
      const current = answers[key] ? answers[key].split(', ') : [];
      const isSelected = current.includes(label);

      let newSelected: string[];
      if (isSelected) {
        newSelected = current.filter((s) => s !== label);
        if (isOther) {
          setOtherInputs((prev) => {
            const next = { ...prev };
            delete next[key];
            return next;
          });
          setAnnotations((prev) => {
            const next = { ...prev };
            delete next[key];
            return next;
          });
        }
      } else {
        newSelected = [...current, label];
      }
      setAnswers((prev) => ({ ...prev, [key]: newSelected.join(', ') }));
    } else {
      setAnswers((prev) => ({ ...prev, [key]: isOther ? '__other__' : label }));

      if (isOther) {
        return;
      }

      // 单选且非 Other：自动跳转到下一个问题
      if (payload && currentStep < payload.questions.length - 1) {
        if (autoAdvanceTimer.current) {
          clearTimeout(autoAdvanceTimer.current);
        }
        autoAdvanceTimer.current = setTimeout(() => {
          setCurrentStep((prev) => prev + 1);
          autoAdvanceTimer.current = null;
        }, 300);
      }
    }
  }, [answers, payload, currentStep]);

  // 处理 Other 输入变化
  const handleOtherInputChange = useCallback((questionKey: string, value: string) => {
    setOtherInputs((prev) => ({ ...prev, [questionKey]: value }));
  }, []);

  // Other 输入确认
  const handleOtherInputConfirm = useCallback((questionKey: string) => {
    const text = otherInputs[questionKey]?.trim();
    if (!text) return;

    const question = payload?.questions.find((q) => q.question === questionKey);
    if (!question) return;

    if (question.multiSelect) {
      const current = answers[questionKey] ? answers[questionKey].split(', ') : [];
      const newSelected = current.filter((s) => s !== 'Other');
      newSelected.push(text);
      setAnswers((prev) => ({ ...prev, [questionKey]: newSelected.join(', ') }));
    } else {
      setAnswers((prev) => ({ ...prev, [questionKey]: text }));
    }

    setAnnotations((prev) => ({
      ...prev,
      [questionKey]: { notes: text },
    }));
  }, [otherInputs, answers, payload]);

  // 上一步
  const handlePrev = useCallback(() => {
    if (currentStep > 0) {
      setCurrentStep((prev) => prev - 1);
    }
  }, [currentStep]);

  // 下一步
  const handleNext = useCallback(() => {
    if (payload && currentStep < payload.questions.length - 1) {
      setCurrentStep((prev) => prev + 1);
    }
  }, [payload, currentStep]);

  // 无待回答事件时不渲染
  if (!payload || payload.questions.length === 0) return null;

  const questions = payload.questions;
  const currentQuestion = questions[currentStep];
  const isLastStep = currentStep === questions.length - 1;
  const isFirstStep = currentStep === 0;
  const totalSteps = questions.length;

  // 当前问题是否已作答
  const currentAnswer = answers[currentQuestion.question];
  const isCurrentAnswered = Boolean(currentAnswer && currentAnswer !== '__other__')
    || (currentAnswer === '__other__' && Boolean(otherInputs[currentQuestion.question]?.trim()));

  // 所有问题是否都已作答
  const allAnswered = questions.every((q) => {
    const ans = answers[q.question];
    return (ans && ans !== '__other__') || (ans === '__other__' && Boolean(otherInputs[q.question]?.trim()));
  });

  // 获取当前问题的选中值
  const selectedValues = (() => {
    const ans = currentAnswer;
    if (!ans) return [];
    if (ans === '__other__') return ['Other'];
    return ans.split(', ');
  })();

  return (
    <div className={styles.questionBar}>
      <div className={styles.stepContent} key={currentStep}>
        <div className={styles.stepHeader}>
          <div className={styles.stepHeaderLabel}>
            <QuestionCircleOutlined />
            <span>{currentQuestion.header}</span>
          </div>
          <span className={styles.stepProgress}>{currentStep + 1}/{totalSteps}</span>
        </div>

        <div className={styles.questionText}>{currentQuestion.question}</div>

        <div className={styles.optionsList}>
          {currentQuestion.options.map((option) => {
            const isSelected = selectedValues.includes(option.label);
            return (
              <div
                key={option.label}
                className={`${styles.optionItem} ${isSelected ? styles.optionItemSelected : ''}`}
                onClick={() => handleSelect(currentQuestion, option.label, false)}
              >
                <div className={styles.optionRadio}>
                  {currentQuestion.multiSelect ? (
                    <Checkbox checked={isSelected} />
                  ) : (
                    <Radio checked={isSelected} />
                  )}
                </div>
                <div className={styles.optionContent}>
                  <div className={styles.optionLabel}>{option.label}</div>
                  {option.description && (
                    <div className={styles.optionDescription}>{option.description}</div>
                  )}
                </div>
              </div>
            );
          })}

          <div
            className={`${styles.optionItem} ${selectedValues.includes('Other') ? styles.optionItemSelected : ''}`}
            onClick={() => handleSelect(currentQuestion, 'Other', true)}
          >
            <div className={styles.optionRadio}>
              {currentQuestion.multiSelect ? (
                <Checkbox checked={selectedValues.includes('Other')} />
              ) : (
                <Radio checked={selectedValues.includes('Other')} />
              )}
            </div>
            <div className={styles.optionContent}>
              <div className={styles.optionLabel}>Other</div>
            </div>
          </div>

          {selectedValues.includes('Other') && (
            <div className={styles.otherInputArea}>
              <Input.TextArea
                className={styles.otherInput}
                value={otherInputs[currentQuestion.question] || ''}
                onChange={(e) => handleOtherInputChange(currentQuestion.question, e.target.value)}
                onBlur={() => handleOtherInputConfirm(currentQuestion.question)}
                onPressEnter={(e) => {
                  if (!e.shiftKey) {
                    e.preventDefault();
                    handleOtherInputConfirm(currentQuestion.question);
                  }
                }}
                placeholder="请输入..."
                autoSize={{ minRows: 1, maxRows: 3 }}
                maxLength={500}
              />
            </div>
          )}
        </div>
      </div>

      <div className={styles.navigation}>
        <Button
          className={styles.navButton}
          disabled={isFirstStep}
          onClick={handlePrev}
        >
          上一个
        </Button>
        {isLastStep ? (
          <Button
            type="primary"
            className={styles.submitButton}
            disabled={!allAnswered || submitting}
            loading={submitting}
            onClick={submitAnswer}
          >
            提交答案
          </Button>
        ) : (
          <Button
            type="primary"
            className={styles.navButton}
            disabled={!isCurrentAnswered}
            onClick={handleNext}
          >
            下一个
          </Button>
        )}
      </div>
    </div>
  );
}
