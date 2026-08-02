import { get, post } from '../utils/request';
import type { ApiResponse, KitFileInfoVO } from '../types';

const FILE_PREFIX = '/kit/file';

export async function uploadSingle(formData: FormData): Promise<ApiResponse<KitFileInfoVO>> {
  return post<KitFileInfoVO>(`${FILE_PREFIX}/upload`, formData as unknown as Record<string, unknown>);
}

export async function createMultipartUpload(formData: FormData): Promise<ApiResponse<Record<string, unknown>>> {
  return post<Record<string, unknown>>(`${FILE_PREFIX}/createMultipartUpload`, formData as unknown as Record<string, unknown>);
}

export async function chunkExist(params: {
  uniqueIdentifier: string;
  uploadId: string;
}): Promise<ApiResponse<number[]>> {
  return get<number[]>(`${FILE_PREFIX}/chunk`, params as Record<string, unknown>);
}

export async function uploadChunk(formData: FormData): Promise<ApiResponse<void>> {
  return post<void>(`${FILE_PREFIX}/chunk`, formData as unknown as Record<string, unknown>);
}

export async function completeMultipartUpload(data: {
  uniqueIdentifier: string;
  uploadId: string;
  fileName: string;
  chunkSize: string;
  disposable?: boolean;
  expiredTime?: string;
  scope?: string;
  visitors?: string;
  categorize?: string;
}): Promise<ApiResponse<KitFileInfoVO>> {
  return post<KitFileInfoVO>(`${FILE_PREFIX}/completeMultipartUpload`, data);
}

export async function getFileInfo(fileId: string): Promise<ApiResponse<KitFileInfoVO>> {
  return post<KitFileInfoVO>(`${FILE_PREFIX}/info/${fileId}`);
}

export async function copyFile(data: {
  sourceFileId: string;
  fileName?: string;
  disposable?: boolean;
  expiredTime?: string;
  scope?: string;
  visitors?: string;
}): Promise<ApiResponse<KitFileInfoVO>> {
  return post<KitFileInfoVO>(`${FILE_PREFIX}/copy`, data);
}

export async function removeFile(fileId: string): Promise<ApiResponse<void>> {
  return post<void>(`${FILE_PREFIX}/remove/${fileId}`);
}
