import {
  CopilotChatMessageView,
  CopilotChatAssistantMessage,
  type CopilotChatAssistantMessageProps,
  type CopilotChatMessageViewProps,
} from '@copilotkit/react-core/v2';
import type { AssistantMessage, ReasoningMessage, Message } from '@ag-ui/core';
import { ReasoningMessageItem } from './ReasoningMessageItem';
import type { ViewConfig } from './types';
import { RAW_ERROR_MESSAGE_NAME } from '@/providers/CopilotKitProvider';
import styles from './copilot-override.module.less';

const DEFAULT_VIEW_CONFIG: ViewConfig = {
  showThinking: true,
  showToolCalls: true,
  showHistory: true,
  enableDragMode: false,
};

export function createCustomRenderMessage(viewConfig: ViewConfig = DEFAULT_VIEW_CONFIG) {
  function CustomAssistantMessage(props: CopilotChatAssistantMessageProps) {
    const { message } = props;
    const isRawError =
      (message as AssistantMessage & { name?: string }).name ===
      RAW_ERROR_MESSAGE_NAME;

    if (!isRawError) {
      return <CopilotChatAssistantMessage {...props} />;
    }

    return (
      <CopilotChatAssistantMessage
        {...props}
        className={styles.rawErrorMessage}
        toolbarVisible={false}
      />
    );
  }

  function CustomReasoningMessage({
    message,
    messages,
    isRunning,
  }: {
    message: ReasoningMessage;
    messages?: Message[];
    isRunning?: boolean;
  }) {
    if (!viewConfig.showThinking) {
      return null;
    }
    const content = typeof message.content === 'string' ? message.content : '';
    const isLatest = messages?.[messages.length - 1]?.id === message.id;
    const isStreaming = !!(isRunning && isLatest);

    return (
      <ReasoningMessageItem
        id={message.id}
        content={content}
        isStreaming={isStreaming}
      />
    );
  }

  function CustomMessageView(props: CopilotChatMessageViewProps) {
    return (
      <CopilotChatMessageView
        {...props}
        assistantMessage={CustomAssistantMessage}
        reasoningMessage={viewConfig.showThinking ? CustomReasoningMessage : (false as any)}
      />
    );
  }

  // 保留 CopilotChatMessageView 的静态属性（如 Cursor），满足 messageView 类型要求
  CustomMessageView.Cursor = CopilotChatMessageView.Cursor;

  return CustomMessageView;
}
