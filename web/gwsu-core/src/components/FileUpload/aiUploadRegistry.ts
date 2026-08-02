import type { FileProperty, KitFileInfoVO } from '../../types';

export interface AiUploadTarget {
  uploadId: string;
  property?: FileProperty;
  attachFile: (fileInfo: KitFileInfoVO) => void;
}

const rawWindow: Window & typeof globalThis & Record<string, unknown> = (0, eval)('window');
const REGISTRY_KEY = '__GWSU_AI_UPLOAD_REGISTRY__';

function getRegistry(): Map<string, AiUploadTarget> {
  const existing = rawWindow[REGISTRY_KEY];
  if (existing instanceof Map) {
    return existing as Map<string, AiUploadTarget>;
  }

  const registry = new Map<string, AiUploadTarget>();
  rawWindow[REGISTRY_KEY] = registry;
  return registry;
}

export function registerAiUploadTarget(target: AiUploadTarget): () => void {
  const registry = getRegistry();
  registry.set(target.uploadId, target);
  return () => {
    const current = registry.get(target.uploadId);
    if (current === target) {
      registry.delete(target.uploadId);
    }
  };
}

export function getAiUploadTarget(uploadId: string): AiUploadTarget | undefined {
  const registry = getRegistry();
  return registry.get(uploadId);
}

export function attachAiUploadedFile(uploadId: string, fileInfo: KitFileInfoVO): void {
  const registry = getRegistry();
  const target = registry.get(uploadId);
  if (!target) {
    throw new Error(`未找到上传控件: ${uploadId}`);
  }
  target.attachFile(fileInfo);
}

export function getAiUploadTargetIds(): string[] {
  return [...getRegistry().keys()];
}
