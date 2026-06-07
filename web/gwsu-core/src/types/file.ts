export enum FileScope {
  PUBLIC = 'PUBLIC',
  PROTECTED = 'PROTECTED',
  PRIVATE = 'PRIVATE',
}

export interface KitFileInfoVO {
  fileId: string;
  fileMetaId: string;
  fileName: string;
  fileSize: string;
  fileSuffix: string;
  disposable: boolean;
  expiredTime: string;
  scope: FileScope;
  visitors: string;
  uniqueId: string;
  uploadServiceType: string;
  fileGroup: string;
  fileUrl: string;
  mediaType: string;
}

export interface FileProperty {
  disposable?: boolean;
  scope?: FileScope;
  visitors?: string;
  categorize?: string;
  expiredTime?: string;
}

export interface ChunkUploadProgress {
  fileId?: string;
  fileName: string;
  fileSize: number;
  uploadedChunks: number;
  totalChunks: number;
  percent: number;
  status: 'pending' | 'uploading' | 'success' | 'error';
  error?: string;
}

export interface FileDownloadProgress {
  fileId: string;
  fileName: string;
  fileSize: number;
  downloadedChunks: number;
  totalChunks: number;
  percent: number;
  status: 'pending' | 'downloading' | 'success' | 'error';
  error?: string;
}

export interface FileUploadOptions {
  property?: FileProperty;
  chunkSize?: number;
  singleUploadThreshold?: number;
  maxConcurrentChunks?: number;
  maxRetryCount?: number;
  onProgress?: (progress: ChunkUploadProgress) => void;
}

export interface FileDownloadOptions {
  chunkSize?: number;
  singleDownloadThreshold?: number;
  maxConcurrentChunks?: number;
  maxRetryCount?: number;
  onProgress?: (progress: FileDownloadProgress) => void;
}
