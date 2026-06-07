import React, { useState, useCallback } from 'react';
import { Button, Progress, message } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { downloadFile } from '../utils/fileDownload';
import type { FileDownloadOptions, FileDownloadProgress } from '../types';

export interface FileDownloadButtonProps {
  fileId: string;
  fileName?: string;
  disabled?: boolean;
  downloadOptions?: FileDownloadOptions;
  children?: React.ReactNode;
}

const FileDownloadButton: React.FC<FileDownloadButtonProps> = ({
  fileId,
  fileName,
  disabled = false,
  downloadOptions,
  children,
}) => {
  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState<FileDownloadProgress | null>(null);

  const handleDownload = useCallback(async () => {
    if (!fileId) return;

    setLoading(true);
    setProgress(null);

    try {
      await downloadFile(fileId, {
        ...downloadOptions,
        onProgress: (p) => {
          setProgress(p);
          downloadOptions?.onProgress?.(p);
        },
      });
      message.success(`${fileName ?? '文件'} 下载完成`);
    } catch (error) {
      message.error(`下载失败: ${(error as Error).message}`);
    } finally {
      setLoading(false);
    }
  }, [fileId, fileName, downloadOptions]);

  return (
    <div style={{ display: 'inline-flex', flexDirection: 'column', gap: 4 }}>
      <Button
        icon={<DownloadOutlined />}
        loading={loading}
        disabled={disabled}
        onClick={handleDownload}
      >
        {children ?? fileName ?? '下载文件'}
      </Button>
      {progress && progress.status === 'downloading' && progress.totalChunks > 1 && (
        <div style={{ width: 160 }}>
          <Progress percent={progress.percent} size="small" />
          <div style={{ fontSize: 12, color: '#999' }}>
            {progress.downloadedChunks}/{progress.totalChunks} 分片已下载
          </div>
        </div>
      )}
    </div>
  );
};

export default FileDownloadButton;
