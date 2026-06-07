import { useState, useCallback, useRef } from 'react';
import { uploadFile } from '../utils/fileUpload';
import type { FileProperty, ChunkUploadProgress, FileUploadOptions, KitFileInfoVO } from '../types';

interface UseFileUploadResult {
  upload: (file: File, options?: FileUploadOptions) => Promise<KitFileInfoVO>;
  progress: ChunkUploadProgress | null;
  abort: () => void;
}

export function useFileUpload(defaultProperty?: FileProperty): UseFileUploadResult {
  const [progress, setProgress] = useState<ChunkUploadProgress | null>(null);
  const abortRef = useRef(false);

  const upload = useCallback(
    async (file: File, options?: FileUploadOptions) => {
      abortRef.current = false;
      setProgress({
        fileName: file.name,
        fileSize: file.size,
        uploadedChunks: 0,
        totalChunks: 0,
        percent: 0,
        status: 'uploading',
      });

      try {
        const result = await uploadFile(file, {
          property: { ...defaultProperty, ...options?.property },
          ...options,
          onProgress: (p) => {
            if (abortRef.current) throw new Error('上传已取消');
            setProgress(p);
            options?.onProgress?.(p);
          },
        });
        return result;
      } catch (error) {
        setProgress((prev) =>
          prev ? { ...prev, status: 'error', error: (error as Error).message } : null,
        );
        throw error;
      }
    },
    [defaultProperty],
  );

  const abort = useCallback(() => {
    abortRef.current = true;
  }, []);

  return { upload, progress, abort };
}
