package org.quyq.gwsu.kit.api.utils;


import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.kit.api.file.FileClientApi;
import org.quyq.gwsu.kit.api.file.dto.ChunkMultipartDTO;
import org.quyq.gwsu.kit.api.file.dto.FileProperty;
import org.quyq.gwsu.kit.api.file.dto.FileUploadDTO;
import org.quyq.gwsu.kit.api.file.enums.FileScope;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FileUtils {

    private FileUtils() {
    }

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);
    //分片上传大小
    private static final long UPLOAD_CHUNK_SIZE = 5L * 1024 * 1024;
    //分片下载配置
    private static final long DOWNLOAD_CHUNK_SIZE = 10L * 1024 * 1024;
    //判断单应用上传和分片上传文件大小配置
    private static final long SINGLE_UPLOAD_THRESHOLD = UPLOAD_CHUNK_SIZE;

    //同时分片上传数限制
    private static final int MAX_CONCURRENT_CHUNKS = 6;

    private static final int MAX_CHUNK_COUNT = 10000;

    private static final int MAX_RETRY_COUNT = 3;

    private static final int CORE_POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors());

    private static final ExecutorService FILE_POOL;

    static {
        FILE_POOL = ThreadPoolUtil.getExecutorService(new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                CORE_POOL_SIZE * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                new FileThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        ));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            FILE_POOL.shutdown();
            try {
                if (!FILE_POOL.awaitTermination(30, TimeUnit.SECONDS)) {
                    FILE_POOL.shutdownNow();
                }
            } catch (InterruptedException e) {
                FILE_POOL.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }, "file-pool-shutdown"));
    }

    private static volatile FileClientApi fileClientApi;

    private static FileClientApi getFileClientApi() {
        if (Objects.nonNull(fileClientApi)) {
            return fileClientApi;
        }
        synchronized (FileUtils.class) {
            if (Objects.isNull(fileClientApi)) {
                fileClientApi = SpringUtils.getBean(FileClientApi.class);
            }
        }
        return fileClientApi;
    }


    // ==================== 上传 API ====================

    public static KitFileInfoVO upload(MultipartFile file) {
        return upload(file, FileProperty.builder().build());
    }

    public static KitFileInfoVO upload(MultipartFile file, FileProperty property) {
        Assert.notNull(file, "上传文件不能为空");
        Assert.notNull(property, "文件属性不能为空");
        long fileSize = file.getSize();
        if (fileSize < SINGLE_UPLOAD_THRESHOLD) {
            return doSingleUpload(file, property);
        }
        return doChunkedUpload(file, property);
    }

    public static KitFileInfoVO upload(File file) {
        return upload(file, FileProperty.builder().build());
    }

    public static KitFileInfoVO upload(File file, FileProperty property) {
        Assert.notNull(file, "上传文件不能为空");
        Assert.notNull(property, "文件属性不能为空");
        Assert.isTrue(file.exists(), "文件不存在: " + file.getAbsolutePath());
        long fileSize = file.length();
        if (fileSize < SINGLE_UPLOAD_THRESHOLD) {
            return doSingleUpload(file, property);
        }
        return doChunkedUpload(file, property);
    }


    // ==================== 下载 API ====================

    public static File download(String fileId) {
        return download(fileId, System.getProperty("user.dir") + File.separator + "download");
    }

    public static File download(String fileId, String rootPath) {
        Assert.hasText(fileId, "文件ID不能为空");
        Assert.hasText(rootPath, "下载目录不能为空");
        KitFileInfoVO fileInfo = getFileInfo(fileId);
        if (Objects.isNull(fileInfo)) {
            return null;
        }
        long fileSize = parseFileSize(fileInfo.getFileSize());
        if (fileSize <= DOWNLOAD_CHUNK_SIZE) {
            return doSingleDownload(fileId, rootPath, fileInfo);
        }
        return doChunkedDownload(fileId, rootPath, fileInfo, fileSize);
    }


    // ==================== 删除 API ====================

    public static void delete(String fileId) {
        Assert.hasText(fileId, "文件ID不能为空");
        FeignUtils.data(getFileClientApi().removeByFileId(fileId));
    }


    // ==================== 文件信息 API ====================

    public static KitFileInfoVO getFileInfo(String fileId) {
        Assert.hasText(fileId, "文件ID不能为空");
        return FeignUtils.data(getFileClientApi().getFileInfo(fileId));
    }


    // ==================== 单点上传 ====================

    private static KitFileInfoVO doSingleUpload(MultipartFile file, FileProperty property) {
        FileUploadDTO dto = buildUploadDTO(file, property);
        return FeignUtils.data(getFileClientApi().uploadSingle(dto));
    }

    private static KitFileInfoVO doSingleUpload(File file, FileProperty property) {
        MultipartFile multipartFile = localToMultipartFile(file);
        return doSingleUpload(multipartFile, property);
    }


    // ==================== 分片上传（MultipartFile） ====================

    private static KitFileInfoVO doChunkedUpload(MultipartFile file, FileProperty property) {
        long fileSize = file.getSize();
        String uniqueIdentifier = getUniqueIdentifier(file);
        byte[] fileBytes = readMultipartFileBytes(file);

        ChunkMultipartDTO form = new ChunkMultipartDTO();
        form.setFileName(file.getOriginalFilename());
        form.setFile(getMultipartHeadStream(fileBytes, file.getOriginalFilename(), file.getContentType()));

        R<Map<String, Object>> multipartInfo = initMultipartUpload(form, property, fileSize, uniqueIdentifier);
        form.setFile(null);

        if (!multipartInfo.isSuccess()) {
            log.warn("分片上传初始化失败，降级为单点上传: {}", multipartInfo.msg());
            return doSingleUpload(file, property);
        }

        fillFormFromProperty(form, property, uniqueIdentifier, multipartInfo.data());

        List<Integer> existChunks = getExistChunks(form);

        Map<Integer, Integer> chunkSizeMap = buildChunkSizeMap(form.parseChunkSize());

        return executeChunkUpload(form, multipartInfo.data(), index -> {
            int offset = calculateChunkOffset(form.parseChunkSize(), index);
            int size = chunkSizeMap.get(index);
            byte[] chunk = new byte[size];
            System.arraycopy(fileBytes, offset, chunk, 0, size);
            return new ChunkMultipartFile(file.getOriginalFilename(), file.getContentType(), chunk);
        }, existChunks);
    }


    // ==================== 分片上传（File） ====================

    private static KitFileInfoVO doChunkedUpload(File file, FileProperty property) {
        long fileSize = file.length();
        String uniqueIdentifier = getUniqueIdentifier(file);

        ChunkMultipartDTO form = new ChunkMultipartDTO();
        form.setFileName(file.getName());
        form.setFile(getFileHeadStream(file));

        R<Map<String, Object>> multipartInfo = initMultipartUpload(form, property, fileSize, uniqueIdentifier);
        form.setFile(null);

        if (!multipartInfo.isSuccess()) {
            log.warn("分片上传初始化失败，降级为单点上传: {}", multipartInfo.msg());
            return doSingleUpload(file, property);
        }

        fillFormFromProperty(form, property, uniqueIdentifier, multipartInfo.data());

        List<Integer> existChunks = getExistChunks(form);

        Map<Integer, Integer> chunkSizeMap = buildChunkSizeMap(form.parseChunkSize());
        String contentType = resolveFileContentType(file);

        return executeChunkUpload(form, multipartInfo.data(), index -> {
            int size = chunkSizeMap.get(index);
            byte[] chunk = readFileChunk(file, index, form.parseChunkSize(), size);
            return new ChunkMultipartFile(file.getName(), contentType, chunk);
        }, existChunks);
    }


    // ==================== 分片上传执行 ====================

    private static KitFileInfoVO executeChunkUpload(
            ChunkMultipartDTO form,
            Map<String, Object> chunkData,
            ChunkExtractor chunkExtractor,
            List<Integer> existChunks
    ) {
        int totalChunks = form.parseChunkSize().size();
        Semaphore semaphore = new Semaphore(MAX_CONCURRENT_CHUNKS);
        List<CompletableFuture<Void>> futures = new ArrayList<>(totalChunks);

        for (int i = 0; i < totalChunks; i++) {
            if (existChunks.contains(i)) {
                log.debug("分片 {} 已上传，跳过", i);
                continue;
            }
            final int index = i;
            futures.add(CompletableFuture.runAsync(
                    () -> {
                        try {
                            semaphore.acquire();
                            try {
                                uploadChunkWithRetry(form, chunkData, chunkExtractor, index);
                            } finally {
                                semaphore.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("分片上传被中断", e);
                        }
                    },
                    FILE_POOL
            ));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            throw new RuntimeException("分片上传失败", e.getCause());
        }

        return FeignUtils.data(getFileClientApi().completeMultipartUpload(form));
    }

    private static void uploadChunkWithRetry(
            ChunkMultipartDTO form,
            Map<String, Object> chunkData,
            ChunkExtractor chunkExtractor,
            int index
    ) {
        String url = (String) chunkData.get("chunk_" + index);
        if (Objects.isNull(url)) {
            throw new RuntimeException("分片 " + index + " 的上传地址不存在");
        }

        String param = url.substring(url.indexOf("?") + 1);
        ChunkMultipartDTO chunkForm = createFormByParam(param);
        chunkForm.setFile(chunkExtractor.extract(index));

        Exception lastException = null;
        for (int retry = 0; retry <= MAX_RETRY_COUNT; retry++) {
            try {
                R<Void> result = getFileClientApi().chunkUpload(chunkForm);
                if (result.isSuccess()) {
                    log.debug("分片 {} 上传完成", index);
                    return;
                }
                lastException = new RuntimeException("分片上传返回失败: " + result.msg());
                log.warn("分片 {} 上传失败(重试 {}/{}): {}", index, retry, MAX_RETRY_COUNT, result.msg());
            } catch (Exception e) {
                lastException = e;
                log.warn("分片 {} 上传异常(重试 {}/{}): {}", index, retry, MAX_RETRY_COUNT, e.getMessage());
            }

            if (retry < MAX_RETRY_COUNT) {
                try {
                    Thread.sleep(1000L * (retry + 1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("分片上传被中断", e);
                }
            }
        }

        throw new RuntimeException("分片 " + index + " 上传失败，已重试 " + MAX_RETRY_COUNT + " 次", lastException);
    }


    // ==================== 单点下载 ====================

    private static File doSingleDownload(String fileId, String rootPath, KitFileInfoVO fileInfo) {
        File dir = ensureDirectory(rootPath);
        File file = new File(dir, fileId + "." + fileInfo.getFileSuffix());

        byte[] data = getFileClientApi().download(fileId, null);
        if (Objects.isNull(data) || data.length == 0) {
            throw new RuntimeException("文件下载失败: " + fileId);
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (IOException e) {
            throw new RuntimeException("文件下载写入失败", e);
        }

        return file;
    }


    // ==================== 分片下载 ====================

    private static File doChunkedDownload(String fileId, String rootPath, KitFileInfoVO fileInfo, long fileSize) {
        long chunkSize = DOWNLOAD_CHUNK_SIZE;
        int chunkCount = (int) (fileSize / chunkSize + (fileSize % chunkSize == 0 ? 0 : 1));

        if (chunkCount > 10) {
            chunkSize = fileSize / 10;
            chunkCount = (int) (fileSize / chunkSize + (fileSize % chunkSize == 0 ? 0 : 1));
        }

        final long finalChunkSize = chunkSize;
        final int finalChunkCount = chunkCount;

        File dir = ensureDirectory(rootPath);
        File file = new File(dir, fileId + "." + fileInfo.getFileSuffix());

        Semaphore semaphore = new Semaphore(MAX_CONCURRENT_CHUNKS);
        List<CompletableFuture<ChunkDownloadResult>> futures = new ArrayList<>(finalChunkCount);

        for (int i = 0; i < finalChunkCount; i++) {
            final int index = i;
            futures.add(CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            semaphore.acquire();
                            try {
                                return downloadChunkWithRetry(fileId, index, finalChunkCount, finalChunkSize, fileSize);
                            } finally {
                                semaphore.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("分片下载被中断", e);
                        }
                    },
                    FILE_POOL
            ));
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            for (CompletableFuture<ChunkDownloadResult> future : futures) {
                ChunkDownloadResult result = future.join();
                raf.seek((long) result.index * finalChunkSize);
                raf.write(result.data);
            }
        } catch (IOException e) {
            throw new RuntimeException("文件下载写入失败", e);
        }

        return file;
    }

    private static ChunkDownloadResult downloadChunkWithRetry(
            String fileId, int index, int totalCount, long chunkSize, long fileSize
    ) {
        long startIndex = (long) index * chunkSize;
        long endIndex = (long) (index + 1) * chunkSize - 1;
        if (index + 1 == totalCount) {
            endIndex = fileSize - 1;
        }

        String range = "bytes=" + startIndex + "-" + endIndex;

        Exception lastException = null;
        for (int retry = 0; retry <= MAX_RETRY_COUNT; retry++) {
            try {
                byte[] data = getFileClientApi().download(fileId, range);
                if (data != null && data.length > 0) {
                    return new ChunkDownloadResult(index, data);
                }
                lastException = new RuntimeException("下载返回空数据");
                log.warn("分片 {} 下载失败(重试 {}/{}): 返回空数据", index, retry, MAX_RETRY_COUNT);
            } catch (Exception e) {
                lastException = e;
                log.warn("分片 {} 下载异常(重试 {}/{}): {}", index, retry, MAX_RETRY_COUNT, e.getMessage());
            }

            if (retry < MAX_RETRY_COUNT) {
                try {
                    Thread.sleep(1000L * (retry + 1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("分片下载被中断", e);
                }
            }
        }

        throw new RuntimeException("分片 " + index + " 下载失败，已重试 " + MAX_RETRY_COUNT + " 次", lastException);
    }


    // ==================== 分片上传初始化 ====================

    private static R<Map<String, Object>> initMultipartUpload(
            ChunkMultipartDTO form, FileProperty property, long fileSize, String uniqueIdentifier
    ) {
        long chunkSize = UPLOAD_CHUNK_SIZE;
        long chunkCount = fileSize % chunkSize == 0 ? fileSize / chunkSize : fileSize / chunkSize + 1;

        if (chunkCount > MAX_CHUNK_COUNT) {
            chunkSize = fileSize / (MAX_CHUNK_COUNT - 1);
            chunkCount = fileSize % chunkSize == 0 ? chunkCount : chunkCount + 1;
        }

        List<Integer> chunkSizes = new ArrayList<>((int) chunkCount);
        for (int i = 0; i < chunkCount; i++) {
            if (i == chunkCount - 1 && fileSize % chunkSize != 0) {
                chunkSizes.add((int) (fileSize % chunkSize));
            } else {
                chunkSizes.add((int) chunkSize);
            }
        }

        form.setUniqueIdentifier(uniqueIdentifier);
        form.applyChunkSizeList(chunkSizes);
        form.setCategorize(property.getCategorize());

        return getFileClientApi().createMultipartUpload(form);
    }


    // ==================== 辅助方法 ====================

    private static FileUploadDTO buildUploadDTO(MultipartFile file, FileProperty property) {
        FileUploadDTO dto = new FileUploadDTO();
        dto.setFile(file);
        dto.setDisposable(property.getDisposable());
        dto.setCategorize(property.getCategorize());
        dto.setScope(property.getScope());
        dto.setVisitors(property.getVisitors());
        if (Objects.nonNull(property.getExpiredTime())) {
            dto.setExpiredTime(property.getExpiredTime());
        }
        return dto;
    }

    private static void fillFormFromProperty(
            ChunkMultipartDTO form, FileProperty property,
            String uniqueIdentifier, Map<String, Object> chunkData
    ) {
        form.setUploadId((String) chunkData.get("uploadId"));
        form.setUniqueIdentifier(uniqueIdentifier);
        form.setDisposable(property.getDisposable());
        form.setCategorize(property.getCategorize());
        form.setScope(property.getScope());
        form.setVisitors(property.getVisitors());
    }

    private static List<Integer> getExistChunks(ChunkMultipartDTO form) {
        try {
            List<Integer> exist = FeignUtils.data(getFileClientApi().chunkExist(form));
            return Objects.nonNull(exist) ? exist : Collections.emptyList();
        } catch (Exception e) {
            log.warn("查询已上传分片失败，将从头上传: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static ChunkMultipartDTO createFormByParam(String param) {
        ChunkMultipartDTO dto = new ChunkMultipartDTO();
        for (String p : param.split("&")) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2) {
                String key = kv[0];
                String value = kv[1];
                switch (key) {
                    case "uniqueIdentifier" -> dto.setUniqueIdentifier(value);
                    case "uploadId" -> dto.setUploadId(value);
                    case "offset" -> {
                        try {
                            dto.setOffset(Integer.parseInt(value));
                        } catch (NumberFormatException _) {
                            log.debug("忽略无法解析的offset参数: {}", value);
                        }
                    }
                    case "fileName" -> dto.setFileName(value);
                    case "categorize" -> dto.setCategorize(value);
                    case "disposable" -> dto.setDisposable(Boolean.parseBoolean(value));
                    case "scope" -> {
                        try {
                            dto.setScope(FileScope.valueOf(value));
                        } catch (IllegalArgumentException _) {
                            log.debug("忽略无法解析的scope参数: {}", value);
                        }
                    }
                    case "visitors" -> dto.setVisitors(value);
                    default -> {
                    }
                }
            }
        }
        return dto;
    }

    private static String getUniqueIdentifier(MultipartFile file) {
        try {
            return DigestUtils.md5DigestAsHex(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("计算文件MD5失败", e);
        }
    }

    private static String getUniqueIdentifier(File file) {
        try (InputStream is = Files.newInputStream(file.toPath())) {
            return DigestUtils.md5DigestAsHex(is);
        } catch (IOException e) {
            throw new RuntimeException("计算文件MD5失败", e);
        }
    }

    private static byte[] readMultipartFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("读取文件字节流失败", e);
        }
    }

    private static ChunkMultipartFile getMultipartHeadStream(byte[] fileBytes, String fileName, String contentType) {
        int headSize = Math.min(1024, fileBytes.length);
        byte[] head = new byte[headSize];
        System.arraycopy(fileBytes, 0, head, 0, headSize);
        return new ChunkMultipartFile(fileName, contentType, head);
    }

    private static ChunkMultipartFile getFileHeadStream(File file) {
        String contentType = resolveFileContentType(file);
        int headSize = (int) Math.min(1024, file.length());
        byte[] head = new byte[headSize];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(head);
        } catch (IOException e) {
            throw new RuntimeException("读取文件头部流失败", e);
        }
        return new ChunkMultipartFile(file.getName(), contentType, head);
    }

    private static ChunkMultipartFile localToMultipartFile(File file) {
        String contentType = resolveFileContentType(file);
        byte[] content = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(content);
        } catch (IOException e) {
            throw new RuntimeException("读取本地文件失败", e);
        }
        return new ChunkMultipartFile(file.getName(), contentType, content);
    }

    private static String resolveFileContentType(File file) {
        return MediaTypeFactory.getMediaType(file.getName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM)
                .toString();
    }

    private static Map<Integer, Integer> buildChunkSizeMap(List<Integer> chunkSizes) {
        Map<Integer, Integer> map = new java.util.HashMap<>(chunkSizes.size());
        for (int i = 0; i < chunkSizes.size(); i++) {
            map.put(i, chunkSizes.get(i));
        }
        return map;
    }

    private static int calculateChunkOffset(List<Integer> chunkSizes, int index) {
        int offset = 0;
        for (int i = 0; i < index; i++) {
            offset += chunkSizes.get(i);
        }
        return offset;
    }

    private static byte[] readFileChunk(File file, int index, List<Integer> chunkSizes, int chunkSize) {
        int offset = calculateChunkOffset(chunkSizes, index);
        byte[] chunk = new byte[chunkSize];
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset);
            raf.readFully(chunk);
        } catch (IOException e) {
            throw new RuntimeException("读取文件分片失败, 分片索引: " + index, e);
        }
        return chunk;
    }

    private static File ensureDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("创建下载目录失败: " + path);
        }
        return dir;
    }

    private static long parseFileSize(String fileSizeStr) {
        try {
            return Long.parseLong(fileSizeStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("文件大小解析失败: " + fileSizeStr, e);
        }
    }


    // ==================== 内部类 ====================

    @FunctionalInterface
    private interface ChunkExtractor {
        ChunkMultipartFile extract(int index);
    }

    private record ChunkDownloadResult(int index, byte[] data) {
    }

    private static class FileThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "file-operation-" + threadNumber.getAndIncrement());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

}
