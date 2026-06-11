import {
  CopilotChatMessageView,
  type CopilotChatMessageViewProps,
} from '@copilotkit/react-core/v2';
import type { ReasoningMessage, Message } from '@ag-ui/core';
import { ReasoningMessageItem } from './ReasoningMessageItem';
import type { ViewConfig } from './types';

const DEFAULT_VIEW_CONFIG: ViewConfig = {
  showThinking: true,
  showToolCalls: true,
  showHistory: true,
  enableDragMode: false,
};

export function createCustomRenderMessage(viewConfig: ViewConfig = DEFAULT_VIEW_CONFIG) {
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
        reasoningMessage={viewConfig.showThinking ? CustomReasoningMessage : (false as any)}
      />
    );
  }

  return CustomMessageView;
}
