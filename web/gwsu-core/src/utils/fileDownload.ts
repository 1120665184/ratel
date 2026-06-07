import * as fileService from '../services/file';
import { downloadRequest, getRequestConfig } from './request';
import type { KitFileInfoVO, FileDownloadOptions, FileDownloadProgress } from '../types';

const DEFAULT_CHUNK_SIZE = 10 * 1024 * 1024;
const DEFAULT_SINGLE_DOWNLOAD_THRESHOLD = DEFAULT_CHUNK_SIZE;
const DEFAULT_MAX_CONCURRENT_CHUNKS = 6;
const DEFAULT_MAX_RETRY_COUNT = 3;

export async function downloadFile(fileId: string, options?: FileDownloadOptions): Promise<void> {
  const opts = normalizeDownloadOptions(options);
  const fileInfo = (await fileService.getFileInfo(fileId)).data;
  const downloadFileName = buildFullFileName(fileInfo);
  const fileSize = Number(fileInfo.fileSize);
  if (fileSize <= opts.singleDownloadThreshold) {
    doSingleDownload(fileId, downloadFileName);
  } else {
    await doChunkedDownload(fileId, downloadFileName, fileInfo, fileSize, opts);
  }
}

function buildFullFileName(fileInfo: KitFileInfoVO): string {
  if (fileInfo.fileSuffix) {
    return `${fileInfo.fileName}.${fileInfo.fileSuffix}`;
  }
  return fileInfo.fileName;
}

function doSingleDownload(fileId: string, fileName: string): void {
  const baseURL = getRequestConfig().baseURL;
  triggerBrowserDownload(`${baseURL}/kit/file/stream/${fileId}`, fileName);
}

async function doChunkedDownload(
  fileId: string,
  fileName: string,
  fileInfo: KitFileInfoVO,
  fileSize: number,
  options: Required<FileDownloadOptions>,
): Promise<void> {
  let chunkSize = options.chunkSize;
  let chunkCount = Math.ceil(fileSize / chunkSize);
  if (chunkCount > 10) {
    chunkSize = Math.ceil(fileSize / 10);
    chunkCount = Math.ceil(fileSize / chunkSize);
  }

  const onProgress = options.onProgress;
  const chunks: ArrayBuffer[] = new Array(chunkCount);
  let downloadedCount = 0;
  const semaphore = createSemaphore(options.maxConcurrentChunks);
  const tasks: Promise<void>[] = [];

  for (let i = 0; i < chunkCount; i++) {
    tasks.push(
      semaphore.acquire().then(async () => {
        try {
          const startIndex = i * chunkSize;
          const endIndex = i + 1 === chunkCount ? fileSize - 1 : (i + 1) * chunkSize - 1;
          chunks[i] = await downloadChunkWithRetry(fileId, startIndex, endIndex, options.maxRetryCount);
          downloadedCount++;
          onProgress?.({
            fileId,
            fileName,
            fileSize,
            downloadedChunks: downloadedCount,
            totalChunks: chunkCount,
            percent: Math.round((downloadedCount / chunkCount) * 100),
            status: 'downloading',
          });
        } finally {
          semaphore.release();
        }
      }),
    );
  }

  await Promise.all(tasks);

  const blob = new Blob(chunks, { type: fileInfo.mediaType || 'application/octet-stream' });
  triggerBlobDownload(blob, fileName);

  onProgress?.({
    fileId,
    fileName,
    fileSize,
    downloadedChunks: chunkCount,
    totalChunks: chunkCount,
    percent: 100,
    status: 'success',
  });
}

async function downloadChunkWithRetry(
  fileId: string,
  startIndex: number,
  endIndex: number,
  maxRetry: number,
): Promise<ArrayBuffer> {
  const rangeHeader = `bytes=${startIndex}-${endIndex}`;
  let lastError: Error | null = null;

  for (let retry = 0; retry <= maxRetry; retry++) {
    try {
      const response = await downloadRequest({
        url: `/kit/file/stream/${fileId}`,
        headers: { Range: rangeHeader },
      });
      if (response.status === 206 || response.status === 200) {
        const buffer = await response.arrayBuffer();
        if (buffer.byteLength > 0) return buffer;
      }
      lastError = new Error(`下载返回空数据，HTTP ${response.status}`);
    } catch (e) {
      lastError = e as Error;
    }
    if (retry < maxRetry) {
      await delay(1000 * (retry + 1));
    }
  }

  const err = new Error(`分片下载失败 [${rangeHeader}]，已重试 ${maxRetry} 次`);
  throw err;
}

function triggerBrowserDownload(url: string, fileName: string) {
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  a.style.display = 'none';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}

function triggerBlobDownload(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  triggerBrowserDownload(url, fileName);
  setTimeout(() => URL.revokeObjectURL(url), 10000);
}

function createSemaphore(max: number) {
  let current = 0;
  const queue: (() => void)[] = [];
  return {
    acquire: () =>
      new Promise<void>((resolve) => {
        if (current < max) {
          current++;
          resolve();
        } else {
          queue.push(() => {
            current++;
            resolve();
          });
        }
      }),
    release: () => {
      current--;
      queue.shift()?.();
    },
  };
}

function delay(ms: number) {
  return new Promise((r) => setTimeout(r, ms));
}

function normalizeDownloadOptions(options?: FileDownloadOptions): Required<FileDownloadOptions> {
  return {
    chunkSize: options?.chunkSize ?? DEFAULT_CHUNK_SIZE,
    singleDownloadThreshold: options?.singleDownloadThreshold ?? DEFAULT_SINGLE_DOWNLOAD_THRESHOLD,
    maxConcurrentChunks: options?.maxConcurrentChunks ?? DEFAULT_MAX_CONCURRENT_CHUNKS,
    maxRetryCount: options?.maxRetryCount ?? DEFAULT_MAX_RETRY_COUNT,
    onProgress: options?.onProgress ?? (() => {}),
  };
}
