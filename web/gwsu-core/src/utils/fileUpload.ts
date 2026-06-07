import md5 from 'js-md5';
import * as fileService from '../services/file';
import type { KitFileInfoVO, FileProperty, FileUploadOptions, ChunkUploadProgress } from '../types';

const DEFAULT_CHUNK_SIZE = 900 * 1024;
const DEFAULT_SINGLE_UPLOAD_THRESHOLD = 900 * 1024;
const DEFAULT_MAX_CONCURRENT_CHUNKS = 6;
const DEFAULT_MAX_RETRY_COUNT = 3;
const MAX_CHUNK_COUNT = 10000;
const HEAD_STREAM_SIZE = 1024;

export async function uploadFile(file: File, options?: FileUploadOptions): Promise<KitFileInfoVO> {
  const opts = normalizeUploadOptions(options);
  if (file.size < opts.singleUploadThreshold) {
    return doSingleUpload(file, opts.property);
  }
  return doChunkedUpload(file, opts);
}

async function doSingleUpload(file: File, property?: FileProperty): Promise<KitFileInfoVO> {
  const formData = new FormData();
  formData.append('file', file);
  appendPropertyToFormData(formData, property);
  const res = await fileService.uploadSingle(formData);
  return res.data;
}

async function doChunkedUpload(file: File, options: Required<FileUploadOptions>): Promise<KitFileInfoVO> {
  const fileSize = file.size;
  const uniqueIdentifier = await computeFileMD5(file);
  const onProgress = options.onProgress;

  const chunkSizes = calculateChunkSizes(fileSize, options.chunkSize);
  const headBlob = file.slice(0, HEAD_STREAM_SIZE);

  const initForm = new FormData();
  initForm.append('file', headBlob, file.name);
  initForm.append('uniqueIdentifier', uniqueIdentifier);
  initForm.append('fileName', file.name);
  initForm.append('chunkSize', chunkSizes.join(','));
  if (options.property?.categorize) {
    initForm.append('categorize', options.property.categorize);
  }

  const initResult = await fileService.createMultipartUpload(initForm);
  if (!initResult.data || (initResult.code !== 0 && initResult.code !== 200)) {
    return doSingleUpload(file, options.property);
  }

  const chunkData = initResult.data;
  const uploadId = chunkData.uploadId as string;
  const existChunks = await getExistChunks(uniqueIdentifier, uploadId);

  const totalChunks = chunkSizes.length;
  let uploadedCount = existChunks.length;
  const semaphore = createSemaphore(options.maxConcurrentChunks);
  const tasks: Promise<void>[] = [];

  for (let i = 0; i < totalChunks; i++) {
    if (existChunks.includes(i)) continue;
    tasks.push(
      semaphore.acquire().then(async () => {
        try {
          await uploadChunkWithRetry(file, i, chunkSizes, chunkData, options);
          uploadedCount++;
          onProgress?.({
            fileName: file.name,
            fileSize,
            uploadedChunks: uploadedCount,
            totalChunks,
            percent: Math.round((uploadedCount / totalChunks) * 100),
            status: 'uploading',
          });
        } finally {
          semaphore.release();
        }
      }),
    );
  }

  await Promise.all(tasks);

  const completeResult = await fileService.completeMultipartUpload({
    uniqueIdentifier,
    uploadId,
    fileName: file.name,
    chunkSize: chunkSizes.join(','),
    disposable: options.property?.disposable,
    scope: options.property?.scope,
    visitors: options.property?.visitors,
    categorize: options.property?.categorize,
  });

  onProgress?.({
    fileId: completeResult.data?.fileId,
    fileName: file.name,
    fileSize,
    uploadedChunks: totalChunks,
    totalChunks,
    percent: 100,
    status: 'success',
  });

  return completeResult.data;
}

async function uploadChunkWithRetry(
  file: File,
  index: number,
  chunkSizes: number[],
  chunkData: Record<string, unknown>,
  options: Required<FileUploadOptions>,
): Promise<void> {
  const chunkUrl = chunkData[`chunk_${index}`] as string;
  if (!chunkUrl) throw new Error(`分片 ${index} 的上传地址不存在`);

  const paramStr = chunkUrl.includes('?') ? chunkUrl.substring(chunkUrl.indexOf('?') + 1) : '';
  const params = parseUrlParams(paramStr);

  let lastError: Error | null = null;
  for (let retry = 0; retry <= options.maxRetryCount; retry++) {
    try {
      const offset = calculateChunkOffset(chunkSizes, index);
      const chunkBlob = file.slice(offset, offset + chunkSizes[index]);
      const formData = new FormData();
      formData.append('file', chunkBlob, file.name);
      Object.entries(params).forEach(([key, value]) => {
        formData.append(key, value);
      });
      const result = await fileService.uploadChunk(formData);
      if (result.code === 0 || result.code === 200) return;
      lastError = new Error(`分片上传返回失败: ${result.msg}`);
    } catch (e) {
      lastError = e as Error;
    }
    if (retry < options.maxRetryCount) {
      await delay(1000 * (retry + 1));
    }
  }

  const err = new Error(`分片 ${index} 上传失败，已重试 ${options.maxRetryCount} 次`);
  throw err;
}

function calculateChunkSizes(fileSize: number, chunkSize: number): number[] {
  let cs = chunkSize;
  let count = Math.ceil(fileSize / cs);
  if (count > MAX_CHUNK_COUNT) {
    cs = Math.ceil(fileSize / (MAX_CHUNK_COUNT - 1));
    count = Math.ceil(fileSize / cs);
  }
  const sizes: number[] = [];
  for (let i = 0; i < count; i++) {
    sizes.push(i === count - 1 && fileSize % cs !== 0 ? fileSize % cs : cs);
  }
  return sizes;
}

function calculateChunkOffset(chunkSizes: number[], index: number): number {
  let offset = 0;
  for (let i = 0; i < index; i++) offset += chunkSizes[i];
  return offset;
}

async function computeFileMD5(file: File): Promise<string> {
  // @ts-ignore
  return md5(await file.arrayBuffer());
}

async function getExistChunks(uniqueIdentifier: string, uploadId: string): Promise<number[]> {
  try {
    const res = await fileService.chunkExist({ uniqueIdentifier, uploadId });
    return res.data ?? [];
  } catch {
    return [];
  }
}

function parseUrlParams(paramStr: string): Record<string, string> {
  const params: Record<string, string> = {};
  if (!paramStr) return params;
  paramStr.split('&').forEach((pair) => {
    const [key, value] = pair.split('=', 2);
    if (key && value) params[decodeURIComponent(key)] = decodeURIComponent(value);
  });
  return params;
}

function appendPropertyToFormData(formData: FormData, property?: FileProperty) {
  if (!property) return;
  if (property.disposable !== undefined) formData.append('disposable', String(property.disposable));
  if (property.scope) formData.append('scope', property.scope);
  if (property.visitors) formData.append('visitors', property.visitors);
  if (property.categorize) formData.append('categorize', property.categorize);
  if (property.expiredTime) formData.append('expiredTime', property.expiredTime);
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

function normalizeUploadOptions(options?: FileUploadOptions): Required<FileUploadOptions> {
  return {
    property: options?.property ?? {},
    chunkSize: options?.chunkSize ?? DEFAULT_CHUNK_SIZE,
    singleUploadThreshold: options?.singleUploadThreshold ?? DEFAULT_SINGLE_UPLOAD_THRESHOLD,
    maxConcurrentChunks: options?.maxConcurrentChunks ?? DEFAULT_MAX_CONCURRENT_CHUNKS,
    maxRetryCount: options?.maxRetryCount ?? DEFAULT_MAX_RETRY_COUNT,
    onProgress: options?.onProgress ?? (() => {}),
  };
}
