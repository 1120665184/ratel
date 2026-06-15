# CopilotKit 多模态支持源码分析与实现方案

> 基于项目 `@copilotkit/react-core@1.57.1`、`@copilotkit/react-ui@1.57.1`、`@ag-ui/core@0.0.52` 源码分析

---

## 一、CopilotKit 多模态架构总览

CopilotKit 的多模态支持基于 **AG-UI 协议**，核心类型定义在 `@ag-ui/core` 中，由 `@copilotkit/shared` 重新导出。整体架构如下：

```
用户选择文件 → useAttachments Hook → Attachment 状态管理
     ↓
CopilotChat.onSubmitInput → 构建 InputContent[] 多模态消息体
     ↓
agent.addMessage({ content: InputContent[] }) → POST /api/.../copilotKit
     ↓
后端 RunAgentInput.messages → 解析多模态 content → 调用 LLM
```

---

## 二、核心类型定义

### 2.1 InputContent — 多模态消息内容联合类型

来自 `@ag-ui/core`，是消息体的核心类型，支持 5 种模态：

```typescript
type InputContent =
  | TextInputContent
  | ImageInputContent
  | AudioInputContent
  | VideoInputContent
  | DocumentInputContent;
```

各模态结构对比：

| 模态 | type 字段 | source 结构 | metadata |
|------|-----------|-------------|----------|
| 文本 | `"text"` | — | — |
| 图片 | `"image"` | `InputContentSource` | 可选 |
| 音频 | `"audio"` | `InputContentSource` | 可选 |
| 视频 | `"video"` | `InputContentSource` | 可选 |
| 文档 | `"document"` | `InputContentSource` | 可选 |

### 2.2 InputContentSource — 内容来源（二选一）

```typescript
// Base64 数据源
type InputContentDataSource = {
  type: "data";
  value: string;       // Base64 编码的数据
  mimeType: string;    // 如 "image/png", "audio/mp3"
};

// URL 数据源
type InputContentUrlSource = {
  type: "url";
  value: string;       // 文件的 URL 地址
  mimeType?: string;   // 可选
};

type InputContentSource = InputContentDataSource | InputContentUrlSource;
```

### 2.3 各模态 InputContent 完整结构

```typescript
// 文本
type TextInputContent = {
  type: "text";
  text: string;
};

// 图片（ImageInputPart 是 ImageInputContent 的别名）
type ImageInputPart = {
  type: "image";
  source: InputContentSource;
  metadata?: unknown;
};

// 音频
type AudioInputPart = {
  type: "audio";
  source: InputContentSource;
  metadata?: unknown;
};

// 视频
type VideoInputPart = {
  type: "video";
  source: InputContentSource;
  metadata?: unknown;
};

// 文档
type DocumentInputPart = {
  type: "document";
  source: InputContentSource;
  metadata?: unknown;
};
```

### 2.4 AttachmentsConfig — 附件配置

```typescript
interface AttachmentsConfig {
  /** 是否启用附件 */
  enabled: boolean;
  /** MIME 类型过滤，如 "image/*,audio/*,video/*,.pdf"，默认 "*/*" */
  accept?: string;
  /** 最大文件大小（字节），默认 20MB (20 * 1024 * 1024) */
  maxSize?: number;
  /** 自定义上传处理。返回 AttachmentUploadResult 或 Promise */
  onUpload?: (file: File) => AttachmentUploadResult | Promise<AttachmentUploadResult>;
  /** 上传失败回调 */
  onUploadFailed?: (error: AttachmentUploadError) => void;
}
```

### 2.5 Attachment — 附件状态对象

```typescript
type AttachmentModality = "image" | "audio" | "video" | "document";

interface Attachment {
  id: string;
  type: AttachmentModality;
  source: InputContentDataSource | InputContentUrlSource;
  filename?: string;
  size?: number;
  status: "uploading" | "ready";
  thumbnail?: string;              // 视频缩略图
  metadata?: Record<string, unknown>;
}
```

### 2.6 AttachmentUploadResult — 自定义上传结果

```typescript
// 返回 Base64 数据
interface AttachmentUploadDataResult {
  type: "data";
  value: string;       // Base64
  mimeType: string;
  metadata?: Record<string, unknown>;
}

// 返回 URL
interface AttachmentUploadUrlResult {
  type: "url";
  value: string;       // 文件 URL
  mimeType?: string;
  metadata?: Record<string, unknown>;
}

type AttachmentUploadResult = AttachmentUploadDataResult | AttachmentUploadUrlResult;
```

### 2.7 AttachmentUploadError — 上传错误

```typescript
type AttachmentUploadErrorReason = "file-too-large" | "invalid-type" | "upload-failed";

interface AttachmentUploadError {
  reason: AttachmentUploadErrorReason;
  file: File;
  message: string;
}
```

---

## 三、CopilotChat 组件的多模态 Props

### 3.1 CopilotChatProps 定义

```typescript
type CopilotChatProps = Omit<CopilotChatViewProps,
  "messages" | "isRunning" | "suggestions" | "suggestionLoadingIndexes" |
  "onSelectSuggestion" | "attachments" | "onRemoveAttachment" |
  "onAddFile" | "dragOver" | "onDragOver" | "onDragLeave" | "onDrop"
> & {
  agentId?: string;
  threadId?: string;
  labels?: Partial<CopilotChatLabels>;
  chatView?: SlotValue<typeof CopilotChatView>;
  isModalDefaultOpen?: boolean;
  /** Enable multimodal file attachments (images, audio, video, documents). */
  attachments?: AttachmentsConfig;
  onError?: (event: { error: Error; code: CopilotKitCoreErrorCode; context: Record<string, any> }) => void | Promise<void>;
  throttleMs?: number;
};
```

### 3.2 CopilotChat 内部多模态消息发送逻辑

CopilotChat 的 `onSubmitInput` 回调已内置多模态处理：

```typescript
const onSubmitInput = useCallback(async (value: string) => {
  // 阻止上传中的附件发送
  const hasUploading = selectedAttachments.some((a) => a.status === "uploading");
  if (hasUploading) return;

  // 消费已就绪的附件
  const readyAttachments = consumeAttachments();

  if (readyAttachments.length > 0) {
    // 多模态消息：构建 InputContent 数组
    const contentParts: InputContent[] = [];
    if (value.trim()) {
      contentParts.push({ type: "text", text: value });
    }
    for (const att of readyAttachments) {
      contentParts.push({
        type: att.type,
        source: att.source,
        metadata: {
          ...(att.filename ? { filename: att.filename } : {}),
          ...att.metadata,
        },
      } as InputContent);
    }
    agent.addMessage({
      id: randomUUID(),
      role: "user",
      content: contentParts,    // ← InputContent[] 多模态数组
    });
  } else {
    // 纯文本消息
    agent.addMessage({
      id: randomUUID(),
      role: "user",
      content: value,           // ← string
    });
  }
}, [agent, selectedAttachments, consumeAttachments]);
```

### 3.3 CopilotChat 内部附件 UI 渲染

CopilotChat 在内部自动处理附件相关的 UI：

- 隐藏的 `<input type="file">` 元素（仅在 `attachmentsEnabled` 时渲染）
- 拖拽区域处理（`onDragOver`/`onDragLeave`/`onDrop`）
- 附件队列渲染（`CopilotChatAttachmentQueue`）
- 附件预览渲染（`CopilotChatAttachmentRenderer`）
- 输入框中的添加文件按钮（`AddMenuButton`）

---

## 四、useAttachments Hook 完整能力

| 功能 | 方法 | 说明 |
|------|------|------|
| 文件选择 | `handleFileUpload` | `<input type="file">` onChange 处理 |
| 拖拽上传 | `handleDragOver/Drop/Leave` | 拖拽文件到聊天区域 |
| 粘贴上传 | 内置 paste 监听 | 在输入框内粘贴图片自动识别 |
| 自定义上传 | `config.onUpload` | 可返回 URL 或 Base64 |
| 文件校验 | `accept` + `maxSize` | MIME 类型过滤 + 大小限制 |
| 缩略图 | `generateVideoThumbnail` | 视频文件自动生成缩略图 |
| 状态管理 | `status: "uploading"/"ready"` | 上传中/就绪状态跟踪 |
| 附件消费 | `consumeAttachments()` | 发送时取出就绪附件并清空队列 |

### 4.1 默认上传行为（无 onUpload）

当不提供 `onUpload` 时，Hook 自动将文件读取为 Base64：

```typescript
if (cfg?.onUpload) {
  const { metadata: meta, ...uploadSource } = await cfg.onUpload(file);
  source = uploadSource;
} else {
  // 默认行为：读取为 Base64
  const base64 = await readFileAsBase64(file);
  source = { type: "data", value: base64, mimeType: file.type };
}
```

### 4.2 MIME 类型到模态的映射

CopilotKit 通过 `getModalityFromMimeType` 自动判断文件模态类型：

- `image/*` → `"image"`
- `audio/*` → `"audio"`
- `video/*` → `"video"`
- 其他 → `"document"`

---

## 五、当前项目实现现状

### 5.1 前端现状

当前 `CopilotChatPanel.tsx` 使用 `@copilotkit/react-ui` 的 CopilotChat，**未传递 `attachments` prop**：

```tsx
// 当前代码 - 没有多模态支持
<CopilotChat
  labels={{
    title: '智能助手',
    placeholder: '输入消息...',
    initial: '我是你的平台助手，有什么问题可以问我哦^_^',
  }}
  className={styles.copilotChat}
  RenderMessage={CustomRenderMessage}
  onStopGeneration={() => {
    agent.abortRun();
  }}
  // ← 缺少 attachments prop
/>
```

### 5.2 后端现状

当前 `CopilotKitInfo.Capabilities` 只有 `threads` 和 `generativeUi`，未声明多模态能力：

```java
public static class Capabilities {
    public boolean threads;
    public boolean generativeUi;
    // ← 缺少 multimodal 声明
}
```

---

## 六、实现方案

### 方案一：最小改动 — 直接给 CopilotChat 加 `attachments` prop

适用于快速验证，使用默认 Base64 内联方式：

```tsx
<CopilotChat
  labels={{
    title: '智能助手',
    placeholder: '输入消息...',
    initial: '我是你的平台助手，有什么问题可以问我哦^_^',
  }}
  className={styles.copilotChat}
  RenderMessage={CustomRenderMessage}
  onStopGeneration={() => {
    agent.abortRun();
  }}
  attachments={{
    enabled: true,
    accept: "image/*,audio/*,video/*,.pdf,.doc,.docx,.txt",
    maxSize: 20 * 1024 * 1024,  // 20MB
  }}
/>
```

**优点**：改动极小，无需后端文件上传接口
**缺点**：大文件会导致消息体过大，Base64 编码增加约 33% 体积

### 方案二：自定义上传到 OSS（生产推荐）

上传文件到 OSS 后返回 URL 引用，减少消息传输体积：

```tsx
<CopilotChat
  labels={{
    title: '智能助手',
    placeholder: '输入消息...',
    initial: '我是你的平台助手，有什么问题可以问我哦^_^',
  }}
  className={styles.copilotChat}
  RenderMessage={CustomRenderMessage}
  onStopGeneration={() => {
    agent.abortRun();
  }}
  attachments={{
    enabled: true,
    accept: "image/*,audio/*,video/*,.pdf,.doc,.docx",
    maxSize: 50 * 1024 * 1024,  // 50MB
    onUpload: async (file) => {
      const formData = new FormData();
      formData.append('file', file);
      const tokenInfo = useUserStore.getState().getTokenInfo();
      const response = await fetch('/api/file/upload', {
        method: 'POST',
        body: formData,
        headers: {
          Authorization: tokenInfo?.token ? `Bearer ${tokenInfo.token}` : '',
        },
      });
      const result = await response.json();
      return {
        type: "url" as const,
        value: result.data.url,
        mimeType: file.type,
        metadata: { filename: file.name },
      };
    },
    onUploadFailed: (error) => {
      console.error('Upload failed:', error);
    },
  }}
/>
```

**优点**：消息体精简，支持大文件，URL 可被 LLM 直接访问
**缺点**：需要后端提供文件上传接口

### 方案三：迁移到 v2 CopilotChat（完整体验）

v2 CopilotChat 来自 `@copilotkit/react-core/v2`，提供更完整的多模态体验（包括音频转写等）：

```tsx
// 修改导入
import { CopilotChat } from '@copilotkit/react-core/v2';

// 使用 v2 API
<CopilotChat
  agentId="brain"
  labels={{
    title: '智能助手',
    placeholder: '输入消息...',
    initial: '我是你的平台助手，有什么问题可以问我哦^_^',
  }}
  RenderMessage={CustomRenderMessage}
  onStopGeneration={() => {
    agent.abortRun();
  }}
  attachments={{
    enabled: true,
    accept: "image/*,audio/*,video/*,.pdf,.doc,.docx",
    maxSize: 50 * 1024 * 1024,
    onUpload: async (file) => {
      // OSS 上传逻辑
    },
    onUploadFailed: (error) => {
      message.error(`文件上传失败: ${error.message}`);
    },
  }}
/>
```

**注意**：v2 CopilotChat 的 API 与 v1 有差异（如 `onStopGeneration` → `onStop`），需要适配。

---

## 七、后端需要的改动

### 7.1 CopilotKitInfo 增加多模态能力声明

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class Capabilities {
    public boolean threads;
    public boolean generativeUi;
    public MultimodalCapabilities multimodal;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class MultimodalCapabilities {
    public MultimodalInputCapabilities input;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class MultimodalInputCapabilities {
    public boolean image;
    public boolean audio;
    public boolean video;
    public boolean document;
}
```

在 `handleInfo()` 中初始化：

```java
@Override
protected CopilotKitInfo handleInfo() {
    return new CopilotKitInfo()
        .addAgent(new CopilotKitInfo.Agents("brain", "智能助手"))
        .setCapabilities(CopilotKitInfo.Capabilities.builder()
            .threads(true)
            .generativeUi(true)
            .multimodal(CopilotKitInfo.MultimodalCapabilities.builder()
                .input(CopilotKitInfo.MultimodalInputCapabilities.builder()
                    .image(true)
                    .audio(true)
                    .video(true)
                    .document(true)
                    .build())
                .build())
            .build());
}
```

### 7.2 消息解析适配

当前后端处理 `RunAgentInput` 时，`messages` 的 `content` 字段可能是：

- **纯文本**：`content: "你好"` (string)
- **多模态**：`content: [{ type: "text", text: "分析这张图" }, { type: "image", source: { type: "data", value: "base64...", mimeType: "image/png" } }]` (InputContent[])

后端需要：

1. 判断 `content` 类型（string vs array）
2. 解析 `InputContent[]` 数组
3. 转换为 LLM 多模态格式，例如 OpenAI 格式：

```json
{
  "role": "user",
  "content": [
    { "type": "text", "text": "分析这张图" },
    { "type": "image_url", "image_url": { "url": "data:image/png;base64,..." } }
  ]
}
```

### 7.3 文件上传接口（方案二/三需要）

提供 REST API 用于文件上传到 OSS：

```
POST /api/file/upload
Content-Type: multipart/form-data

Response:
{
  "code": 200,
  "data": {
    "url": "https://oss.example.com/files/xxx.png",
    "filename": "screenshot.png"
  }
}
```

---

## 八、数据流对比

### 当前（纯文本）

```
用户输入 "你好"
  → content: "你好"
  → 后端直接作为 LLM prompt
```

### 多模态（改造后）

```
用户输入 "分析这张图" + 选择图片
  → content: [
      { type: "text", text: "分析这张图" },
      {
        type: "image",
        source: { type: "data", value: "base64...", mimeType: "image/png" },
        metadata: { filename: "screenshot.png" }
      }
    ]
  → 后端解析 content 数组
  → 转换为 LLM 多模态格式
```

---

## 九、音频转写功能

CopilotKit 还内置了音频转写（语音输入）功能，通过 `audioFileTranscriptionEnabled` 控制：

- 前端：`copilotkit.audioFileTranscriptionEnabled` 读取后端 info 接口返回的配置
- 后端：在 `handleInfo()` 返回的 JSON 中包含 `audioFileTranscriptionEnabled: true/false`
- 启用后，CopilotChat 输入框会显示麦克风按钮，支持录音转文字

当前项目未配置此功能。如需启用，需在后端 info 响应中添加：

```json
{
  "version": "1.0.0",
  "agents": { ... },
  "audioFileTranscriptionEnabled": true,
  "capabilities": { ... }
}
```

---

## 十、关键源码文件索引

| 文件 | 路径 | 说明 |
|------|------|------|
| CopilotKitProvider | `web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx` | Provider 封装 |
| CopilotChatPanel | `web/apps/gwsu-main/src/components/AIChat/CopilotChatPanel.tsx` | 聊天面板 |
| CustomRenderMessage | `web/apps/gwsu-main/src/components/AIChat/CustomRenderMessage.tsx` | 消息渲染 |
| CopilotChat (v2) | `@copilotkit/react-core/src/v2/components/chat/CopilotChat.tsx` | v2 聊天组件 |
| useAttachments | `@copilotkit/react-core/src/v2/hooks/use-attachments.tsx` | 附件 Hook |
| AttachmentsConfig | `@copilotkit/shared/src/attachments/types.ts` | 附件类型定义 |
| InputContent | `@ag-ui/core/dist/index.d.ts` | 多模态消息类型 |
| Message types | `@copilotkit/shared/src/types/message.ts` | 消息类型重导出 |
| AguiController | `common/common-ai/.../AguiController.java` | 后端 AG-UI 控制器 |
| CopilotKitInfo | `common/common-ai/.../CopilotKitInfo.java` | 后端能力声明 |

---

## 十一、方案对比总结

| 维度 | 方案一（Base64 内联） | 方案二（OSS 上传） | 方案三（v2 迁移） |
|------|----------------------|-------------------|-------------------|
| 前端改动量 | 极小（加 1 个 prop） | 小（加 prop + onUpload） | 中（改导入 + 适配 API） |
| 后端改动量 | 中（消息解析） | 中（消息解析 + 上传接口） | 中（消息解析 + 上传接口） |
| 大文件支持 | 差（Base64 膨胀） | 好（URL 引用） | 好（URL 引用） |
| 音频转写 | 不支持 | 不支持 | 支持 |
| 推荐场景 | 快速验证 | 生产环境 | 需要完整多模态体验 |
