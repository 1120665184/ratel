export type FileServiceType = 'LOCAL' | 'MINIO' | 'OSS' | 'COS';

export interface LocalConfig {
  path: string;
}

export interface MinioConfig {
  url: string;
  accessKey: string;
  secretKey: string;
}

export interface OssConfig {
  endpoint: string;
  accessKey: string;
  secretKey: string;
}

export interface CosConfig {
  endpoint: string;
  accessKey: string;
  secretKey: string;
  region: string;
}

export interface UploadServerConfig {
  type: FileServiceType;
  group: string;
  local: LocalConfig;
  minio: MinioConfig;
  oss: OssConfig;
  cos: CosConfig;
}

export interface ExtensionFilterConfig {
  enabled: boolean;
  disable: string;
}

export type AttachmentTabKey = 'server' | 'filter';

export const UPLOAD_SERVER_CONFIG_KEY = 'upload_server_info_config';
export const UPLOAD_EXTENSION_CONFIG_KEY = 'upload_file_extension_config';

export function createDefaultServerConfig(): UploadServerConfig {
  return {
    type: 'LOCAL',
    group: 'common',
    local: { path: '/data/upload' },
    minio: { url: '', accessKey: '', secretKey: '' },
    oss: { endpoint: '', accessKey: '', secretKey: '' },
    cos: { endpoint: '', accessKey: '', secretKey: '', region: '' },
  };
}

export function createDefaultExtensionConfig(): ExtensionFilterConfig {
  return { enabled: false, disable: '' };
}

export const FILE_SERVICE_TYPE_OPTIONS: { value: FileServiceType; label: string }[] = [
  { value: 'LOCAL', label: '本地存储' },
  { value: 'MINIO', label: 'MinIO' },
  { value: 'OSS', label: '阿里云 OSS' },
  { value: 'COS', label: '腾讯云 COS' },
];
