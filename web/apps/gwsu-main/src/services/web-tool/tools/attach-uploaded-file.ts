import {
  attachAiUploadedFile,
  copyFile,
  getAiUploadTargetIds,
  getAiUploadTarget,
} from '@gwsu/core';
import type { FileProperty } from '@gwsu/core';
import { registerWebTool } from '../registry';
import type { WebToolExecutor, WebToolResult } from '../types';

function buildCopyPayload(fileId: string, property?: FileProperty) {
  return {
    sourceFileId: fileId,
    disposable: property?.disposable,
    expiredTime: property?.expiredTime,
    scope: property?.scope,
    visitors: property?.visitors,
  };
}

const attachUploadedFileTool: WebToolExecutor = {
  async execute(params: Record<string, unknown>): Promise<WebToolResult> {
    const uploadId = String(params.uploadId ?? '').trim();
    const fileId = String(params.fileId ?? '').trim();

    if (!uploadId) {
      return { success: false, result: '缺少 uploadId 参数' };
    }
    if (!fileId) {
      return { success: false, result: '缺少 fileId 参数' };
    }

    const target = getAiUploadTarget(uploadId);
    if (!target) {
      const availableIds = getAiUploadTargetIds();
      const availableText = availableIds.length > 0
        ? `当前可用 uploadId: ${availableIds.join(', ')}`
        : '当前没有已注册的上传控件';
      return {
        success: false,
        result: `未找到 uploadId 为 ${uploadId} 的上传控件，请先调用GetPageState刷新界面状态。${availableText}`,
      };
    }

    const response = await copyFile(buildCopyPayload(fileId, target.property));
    const copiedFile = response.data;
    if (!copiedFile?.fileId) {
      return { success: false, result: `文件 ${fileId} 复制失败，未返回有效文件信息` };
    }

    attachAiUploadedFile(uploadId, copiedFile);
    return {
      success: true,
      result: `已将文件 ${copiedFile.fileName}(${copiedFile.fileId}) 挂载到上传控件 ${uploadId}`,
    };
  },
};

registerWebTool('AttachUploadedFile', attachUploadedFileTool);
