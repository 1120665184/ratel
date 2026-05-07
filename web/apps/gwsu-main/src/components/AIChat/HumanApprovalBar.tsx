import { Button, Input } from 'antd';
import { SafetyCertificateOutlined, CheckOutlined, CloseOutlined, SendOutlined } from '@ant-design/icons';
import { useState, useEffect, useCallback } from 'react';
import { useAgent } from '@copilotkit/react-core/v2';
import { onHumanApproval, clearHumanApproval, getPendingApproval } from '@/services/human-approval';
import type { HumanApprovalPayload, ApprovalResultType, ApprovalStage } from '@/services/human-approval';
import styles from './HumanApprovalBar.module.less';

/**
 * 嵌入式人工审批卡片组件
 * 展示在聊天输入框上方，供用户对 AI 的操作进行审批或拒绝
 *
 * 两种审批阶段：
 * - POST_REASONING：推理后暂停，用户可审批/拒绝（拒绝时可选填写原因）
 * - POST_ACTING：行动后暂停，用户审批/拒绝（拒绝时直接提交，不需要原因）
 */
export function HumanApprovalBar() {
  const [approvalPayload, setApprovalPayload] = useState<HumanApprovalPayload | null>(null);
  const [showRejectReason, setShowRejectReason] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { agent } = useAgent({ agentId: 'brain' });

  // 监听审批事件
  useEffect(() => {
    const unsubscribe = onHumanApproval((payload) => {
      setApprovalPayload(payload);
      setShowRejectReason(false);
      setRejectReason('');
    });
    return unsubscribe;
  }, []);

  // 初始化时检查是否有待审批事件
  useEffect(() => {
    const pending = getPendingApproval();
    if (pending) {
      setApprovalPayload(pending);
    }
  }, []);

  /**
   * 提交审批结果
   * 将审批消息以 role: 'approval' 的形式发送给 agent，后端识别后从 messages 中移除，不进入上下文历史
   */
  const submitApproval = useCallback(async (result: ApprovalResultType, reason?: string) => {
    if (!approvalPayload) return;

    setSubmitting(true);
    try {
      // 构造审批消息内容
      const approvalContent = result === 'REJECTED' && reason
        ? JSON.stringify({ result, rejectReason: reason })
        : JSON.stringify({ result });

      // 将审批消息追加到 agent 消息列表
      // role: 'approval' — 后端 AguiRequestProcessor 识别后从 messages 中移除，不进入上下文历史
      const approvalMsgId = crypto.randomUUID();
      agent.addMessage({
        id: approvalMsgId,
        role: 'approval',
        content: approvalContent,
      } as any);

      // 触发 agent 继续运行
      await agent.runAgent();

      // 从 agent 消息列表中移除 approval 消息，避免下次请求时带上
      const currentMessages = agent.messages || [];
      const filteredMessages = currentMessages.filter(
        (msg: any) => msg.role !== 'approval'
      );
      if (filteredMessages.length !== currentMessages.length) {
        agent.setMessages(filteredMessages);
      }

      // 清除审批状态
      clearHumanApproval();
      setApprovalPayload(null);
      setShowRejectReason(false);
      setRejectReason('');
    } catch (error) {
      console.error('[HumanApproval] 提交审批结果失败:', error);
    } finally {
      setSubmitting(false);
    }
  }, [approvalPayload, agent]);

  // 点击批准
  const handleApprove = useCallback(() => {
    submitApproval('APPROVED');
  }, [submitApproval]);

  // 点击拒绝
  const handleReject = useCallback(() => {
    const stage = approvalPayload?.stage;
    if (stage === 'POST_REASONING') {
      // POST_REASONING 阶段：展开拒绝原因输入框
      setShowRejectReason(true);
    } else {
      // POST_ACTING 阶段：直接提交拒绝
      submitApproval('REJECTED');
    }
  }, [approvalPayload, submitApproval]);

  // 确认拒绝（带原因）
  const handleConfirmReject = useCallback(() => {
    submitApproval('REJECTED', rejectReason.trim() || undefined);
  }, [submitApproval, rejectReason]);

  // 取消拒绝原因输入
  const handleCancelReject = useCallback(() => {
    setShowRejectReason(false);
    setRejectReason('');
  }, []);

  // 无待审批事件时不渲染
  if (!approvalPayload) return null;

  // 提取展示信息
  const { stage, reasoningStageInfo, actingStageInfo } = approvalPayload;

  const tip = stage === 'POST_REASONING'
    ? reasoningStageInfo?.[0]?.tip ?? 'AI 请求执行操作，请确认是否允许'
    : actingStageInfo?.tip ?? '操作已执行，请确认结果';

  const toolName = stage === 'POST_REASONING'
    ? reasoningStageInfo?.[0]?.toolInfo.name
    : actingStageInfo?.resultInfo.name;

  const toolInput = stage === 'POST_REASONING'
    ? reasoningStageInfo?.[0]?.toolInfo.input
    : null;

  const toolOutput = stage === 'POST_ACTING'
    ? actingStageInfo?.resultInfo.output
    : null;

  // 格式化工具参数摘要
  const formatInputSummary = (input: Record<string, unknown> | null | undefined): string => {
    if (!input) return '';
    const entries = Object.entries(input);
    if (entries.length === 0) return '';
    // 限制显示长度
    const summary = entries.map(([k, v]) => `${k}=${typeof v === 'string' ? v : JSON.stringify(v)}`).join(', ');
    return summary.length > 80 ? summary.slice(0, 80) + '...' : summary;
  };

  // 格式化工具输出摘要
  const formatOutputSummary = (output: { type: string; text: string }[] | null | undefined): string => {
    if (!output) return '';
    const text = output.map(o => o.text).join(' ');
    return text.length > 80 ? text.slice(0, 80) + '...' : text;
  };

  return (
    <div className={styles.approvalBar}>
      <div className={styles.approvalContent}>
        <SafetyCertificateOutlined className={styles.approvalIcon} />
        <div className={styles.approvalInfo}>
          <div className={styles.approvalTip}>{tip}</div>
          <div className={styles.approvalDetail}>
            {toolName && (
              <>
                <span className={styles.toolName}>{toolName}</span>
                {toolInput && formatInputSummary(toolInput) && (
                  <span>{formatInputSummary(toolInput)}</span>
                )}
                {toolOutput && formatOutputSummary(toolOutput) && (
                  <span>{formatOutputSummary(toolOutput)}</span>
                )}
              </>
            )}
          </div>
        </div>
        <div className={styles.approvalActions}>
          <Button
            type="primary"
            size="small"
            className={styles.approveBtn}
            icon={<CheckOutlined />}
            loading={submitting}
            onClick={handleApprove}
          >
            批准
          </Button>
          <Button
            danger
            size="small"
            className={styles.rejectBtn}
            icon={<CloseOutlined />}
            loading={submitting && !showRejectReason}
            onClick={handleReject}
            disabled={showRejectReason}
          >
            拒绝
          </Button>
        </div>
      </div>

      {/* 拒绝原因输入区域 - 仅 POST_REASONING 阶段展开 */}
      {showRejectReason && (
        <div className={styles.rejectReasonArea}>
          <Input.TextArea
            className={styles.rejectInput}
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            placeholder="可选：填写拒绝原因..."
            autoSize={{ minRows: 1, maxRows: 3 }}
            onPressEnter={(e) => {
              if (!e.shiftKey) {
                e.preventDefault();
                handleConfirmReject();
              }
            }}
          />
          <Button
            type="primary"
            danger
            size="small"
            className={styles.rejectConfirmBtn}
            icon={<SendOutlined />}
            loading={submitting}
            onClick={handleConfirmReject}
          >
            提交
          </Button>
          <Button
            size="small"
            className={styles.rejectConfirmBtn}
            onClick={handleCancelReject}
          >
            取消
          </Button>
        </div>
      )}
    </div>
  );
}
