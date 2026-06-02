import { AssistantMessage, UserMessage, type RenderMessageProps } from '@copilotkit/react-ui';
import { ReasoningMessageItem } from './ReasoningMessageItem';
import type { ViewConfig } from './types';

const DEFAULT_VIEW_CONFIG: ViewConfig = {
  showThinking: true,
  showToolCalls: true,
  showHistory: true,
  enableDragMode: false,
};

/**
 * 创建自定义消息渲染组件
 * 通过闭包注入 viewConfig，避免修改 CopilotKit 的 RenderMessage 接口
 */
export function createCustomRenderMessage(viewConfig: ViewConfig = DEFAULT_VIEW_CONFIG) {
  function CustomRenderMessage(props: RenderMessageProps) {
    const {
      message,
      messages,
      inProgress,
      index,
      isCurrentMessage,
      onRegenerate,
      onCopy,
      onThumbsUp,
      onThumbsDown,
      messageFeedback,
      markdownTagRenderers,
      ImageRenderer,
    } = props;

    // 推理/思考消息：不展示时直接不渲染组件
    if (message.role === 'reasoning') {
      if (!viewConfig.showThinking) {
        return null;
      }
      const content = typeof message.content === 'string' ? message.content : '';
      const isStreaming = inProgress && isCurrentMessage;

      return (
        <ReasoningMessageItem
          id={message.id}
          content={content}
          isStreaming={isStreaming}
        />
      );
    }

    // 用户消息
    if (message.role === 'user') {
      return (
        <UserMessage
          key={index}
          rawData={message}
          data-message-role="user"
          message={message}
          ImageRenderer={ImageRenderer}
        />
      );
    }

    // 助手消息
    if (message.role === 'assistant') {
      return (
        <AssistantMessage
          key={index}
          data-message-role="assistant"
          subComponent={message.generativeUI?.()}
          rawData={message}
          message={message}
          messages={messages}
          isLoading={inProgress && isCurrentMessage && !message.content}
          isGenerating={inProgress && isCurrentMessage && !!message.content}
          isCurrentMessage={isCurrentMessage}
          onRegenerate={() => onRegenerate?.(message.id)}
          onCopy={onCopy}
          onThumbsUp={onThumbsUp}
          onThumbsDown={onThumbsDown}
          feedback={messageFeedback?.[message.id] || null}
          markdownTagRenderers={markdownTagRenderers}
          ImageRenderer={ImageRenderer}
        />
      );
    }

    return null;
  }

  return CustomRenderMessage;
}
