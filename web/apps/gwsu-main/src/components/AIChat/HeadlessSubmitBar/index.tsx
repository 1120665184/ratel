import { CheckCircleOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { Button } from 'antd';
import { useAgent } from '@copilotkit/react-core/v2';
import { useCopilotKit } from '@copilotkit/react-core/v2';
import {
  clearHumanApproval,
} from '@/services/human-approval';
import { clearAskUserQuestion } from '@/services/ask-user-question';
import styles from './index.module.less';

/**
 * 无头浏览器提交控制条
 *
 * 当后端（HeadlessBrowserSession）需要提交审批或回答问题时，
 * 通过 page.evaluate() 设置 data-headless-forms-visible 属性来显示此组件，
 * 然后使用 page.locator().click() 点击可见按钮（Playwright 原生 click 会协调事件循环，避免死锁），
 * 点击后自动隐藏。
 *
 * 隐藏的 input 用于接收后端填充的值，按钮负责触发提交逻辑。
 */
export function HeadlessSubmitBar() {
  const { agent } = useAgent({ agentId: 'brain' });
  const { copilotkit } = useCopilotKit();

  return (
    <div className={styles.submitBar} data-testid="headless-forms">
      {/* 隐藏输入框 - 接收后端填充的值 */}
      <input data-testid="headless-approval-result" readOnly className={styles.hiddenInput} />
      <input data-testid="headless-approval-reject-reason" readOnly className={styles.hiddenInput} />
      <input data-testid="headless-question-answers" readOnly className={styles.hiddenInput} />
      <input data-testid="headless-question-tool-call-id" readOnly className={styles.hiddenInput} />

      {/* 可见按钮区 */}
      <div className={styles.buttonRow}>
        <Button
          type="primary"
          size="small"
          className={styles.submitBtn}
          icon={<SafetyCertificateOutlined />}
          data-testid="headless-approval-submit"
          onClick={async () => {
            const resultEl = document.querySelector<HTMLInputElement>(
              '[data-testid="headless-approval-result"]',
            );
            const reasonEl = document.querySelector<HTMLInputElement>(
              '[data-testid="headless-approval-reject-reason"]',
            );
            const result = resultEl?.value;
            if (!result) return;

            const rejectReason = reasonEl?.value ?? '';
            const content =
              result === 'REJECTED' && rejectReason
                ? JSON.stringify({ result, rejectReason })
                : JSON.stringify({ result });

            const msgId = crypto.randomUUID();
            agent.addMessage({ id: msgId, role: 'approval', content } as any);
            clearHumanApproval();

            try {
              await copilotkit.runAgent({ agent });
            } catch (e) {
              console.error('[HeadlessApproval] runAgent失败:', e);
            }

            // 清除 agent 消息列表中的 approval 消息，避免下次请求时带上
            const currentMessages = agent.messages || [];
            const filteredMessages = currentMessages.filter(
              (msg: any) => msg.role !== 'approval',
            );
            if (filteredMessages.length !== currentMessages.length) {
              agent.setMessages(filteredMessages);
            }

            // 重置表单值并隐藏
            if (resultEl) resultEl.value = '';
            if (reasonEl) reasonEl.value = '';
            document.body.removeAttribute('data-headless-forms-visible');
          }}
        >
          提交审批
        </Button>
        <Button
          type="primary"
          size="small"
          className={styles.submitBtn}
          icon={<CheckCircleOutlined />}
          data-testid="headless-question-submit"
          onClick={async () => {
            const answersEl = document.querySelector<HTMLInputElement>(
              '[data-testid="headless-question-answers"]',
            );
            const toolCallIdEl = document.querySelector<HTMLInputElement>(
              '[data-testid="headless-question-tool-call-id"]',
            );
            const answersJson = answersEl?.value;
            const toolCallId = toolCallIdEl?.value;
            if (!answersJson || !toolCallId) return;
            try {
              const answers = JSON.parse(answersJson);
              const answer = { answers, annotations: {} };

              const msgId = crypto.randomUUID();
              agent.addMessage({
                id: msgId,
                role: 'tool',
                content: JSON.stringify(answer),
                toolCallId,
              } as any);
              clearAskUserQuestion();

              try {
                console.log('[Headless] runAgent before');
                await copilotkit.runAgent({ agent });
                console.log('[Headless] runAgent after');
              } catch (e) {
                console.error('[HeadlessQuestion] runAgent失败:', e);
              }
            } catch (e) {
              console.error('[HeadlessQuestion] 提交失败:', e);
            }

            // 重置表单值并隐藏
            if (answersEl) answersEl.value = '';
            if (toolCallIdEl) toolCallIdEl.value = '';
            document.body.removeAttribute('data-headless-forms-visible');
          }}
        >
          提交问题
        </Button>
      </div>
    </div>
  );
}
