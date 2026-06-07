import { useState, useCallback, useRef } from 'react';
import { downloadFile } from '../utils/fileDownload';
import type { FileDownloadOptions, FileDownloadProgress } from '../types';

interface UseFileDownloadResult {
  download: (fileId: string, options?: FileDownloadOptions) => Promise<void>;
  progress: FileDownloadProgress | null;
  abort: () => void;
}

export function useFileDownload(): UseFileDownloadResult {
  const [progress, setProgress] = useState<FileDownloadProgress | null>(null);
  const abortRef = useRef(false);

  const download = useCallback(
    async (fileId: string, options?: FileDownloadOptions) => {
      abortRef.current = false;
      setProgress({
        fileId,
        fileName: '',
        fileSize: 0,
        downloadedChunks: 0,
        totalChunks: 0,
        percent: 0,
        status: 'downloading',
      });

      try {
        await downloadFile(fileId, {
          ...options,
          onProgress: (p) => {
            if (abortRef.current) throw new Error('下载已取消');
            setProgress(p);
            options?.onProgress?.(p);
          },
        });
      } catch (error) {
        setProgress((prev) =>
          prev ? { ...prev, status: 'error', error: (error as Error).message } : null,
        );
        throw error;
      }
    },
    [],
  );

  const abort = useCallback(() => {
    abortRef.current = true;
  }, []);

  return { download, progress, abort };
}
