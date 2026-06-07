import React, { useCallback } from 'react';
import { Upload, Progress, message } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import type { UploadProps, RcFile } from 'antd/es/upload';
import { uploadFile } from '../../utils/fileUpload';
import type { FileProperty, FileUploadOptions, ChunkUploadProgress } from '../../types';

const { Dragger } = Upload;

export interface FileUploadProps {
  property?: FileProperty;
  multiple?: boolean;
  maxCount?: number;
  accept?: string;
  maxSize?: number;
  disabled?: boolean;
  listType?: 'text' | 'picture' | 'picture-card';
  draggable?: boolean;
  value?: string[];
  onChange?: (fileIds: string[]) => void;
  uploadOptions?: FileUploadOptions;
}

interface FileItem {
  uid: string;
  name: string;
  status: 'uploading' | 'done' | 'error';
  percent: number;
  fileId?: string;
}

export const FileUpload: React.FC<FileUploadProps> = ({
  property,
  multiple = false,
  maxCount,
  accept,
  maxSize,
  disabled = false,
  listType = 'text',
  draggable = false,
  value,
  onChange,
  uploadOptions,
}) => {
  const [fileList, setFileList] = React.useState<FileItem[]>([]);
  const [progressMap, setProgressMap] = React.useState<Record<string, ChunkUploadProgress>>({});

  const triggerChange = useCallback(
    (ids: string[]) => {
      onChange?.(ids);
    },
    [onChange],
  );

  const customUpload = useCallback(
    async (options: any) => {
      const { file, onSuccess, onError, onProgress } = options;
      const uid = file.uid;

      try {
        const result = await uploadFile(file, {
          property,
          ...uploadOptions,
          onProgress: (p: ChunkUploadProgress) => {
            setProgressMap((prev) => ({ ...prev, [uid]: p }));
            onProgress?.({ percent: p.percent }, new XMLHttpRequest());
          },
        });

        setFileList((prev) =>
          prev.map((f) => (f.uid === uid ? { ...f, status: 'done' as const, percent: 100, fileId: result.fileId } : f)),
        );

        onSuccess?.(result, new XMLHttpRequest());

        const currentIds = fileList.filter((f) => f.fileId).map((f) => f.fileId!);
        if (result.fileId) {
          currentIds.push(result.fileId);
        }
        triggerChange(currentIds);
      } catch (error) {
        setFileList((prev) =>
          prev.map((f) => (f.uid === uid ? { ...f, status: 'error' as const } : f)),
        );
        onError?.(error as Error);
        message.error(`文件 ${file.name} 上传失败: ${(error as Error).message}`);
      }
    },
    [property, uploadOptions, fileList, triggerChange],
  );

  const handleBeforeUpload = useCallback(
    (file: RcFile) => {
      if (maxSize && file.size > maxSize) {
        message.error(`文件 ${file.name} 超过大小限制 ${Math.round(maxSize / 1024 / 1024)}MB`);
        return false;
      }
      setFileList((prev) => [
        ...prev,
        { uid: file.uid, name: file.name, status: 'uploading', percent: 0 },
      ]);
      return true;
    },
    [maxSize],
  );

  const handleRemove = useCallback(
    (file: any) => {
      setFileList((prev) => prev.filter((f) => f.uid !== file.uid));
      const remainingIds = fileList.filter((f) => f.uid !== file.uid && f.fileId).map((f) => f.fileId!);
      triggerChange(remainingIds);
    },
    [fileList, triggerChange],
  );

  const uploadProps: UploadProps = {
    multiple,
    maxCount,
    accept,
    disabled,
    listType,
    customRequest: customUpload,
    beforeUpload: handleBeforeUpload,
    onRemove: handleRemove,
    fileList: fileList.map((f) => ({
      uid: f.uid,
      name: f.name,
      status: f.status,
      percent: f.percent,
    })),
    itemRender: (originNode, file) => {
      const p = progressMap[file.uid];
      return (
        <div>
          {originNode}
          {p && p.status === 'uploading' && p.totalChunks > 1 && (
            <div style={{ marginTop: 4 }}>
              <Progress percent={p.percent} size="small" />
              <div style={{ fontSize: 12, color: '#999', marginTop: 2 }}>
                {p.uploadedChunks}/{p.totalChunks} 分片已上传
              </div>
            </div>
          )}
        </div>
      );
    },
  };

  if (draggable) {
    return (
      <Dragger {...uploadProps}>
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
        {maxSize && (
          <p className="ant-upload-hint">单个文件不超过 {Math.round(maxSize / 1024 / 1024)}MB</p>
        )}
      </Dragger>
    );
  }

  return <Upload {...uploadProps}>{fileList.length >= (maxCount ?? Infinity) ? null : <a>上传文件</a>}</Upload>;
};
