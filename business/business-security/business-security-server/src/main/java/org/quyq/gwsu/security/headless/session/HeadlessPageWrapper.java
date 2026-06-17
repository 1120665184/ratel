package org.quyq.gwsu.security.headless.session;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.quyq.gwsu.kit.api.file.dto.FileProperty;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.api.utils.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 无头浏览器页面操作包装器
 * <p>
 * 封装 BrowserContext 和 Page 的高级操作，在 HeadlessAgentListener 事件回调中暴露，
 * 供调用方在事件处理过程中对浏览器界面进行操作（截图、录制等）。
 * <p>
 * 功能：
 * 1. 开始屏幕录制（基于定时 page.screenshot()，受 deviceScaleFactor 影响可高 DPI）
 * 2. 结束屏幕录制（将截图帧序列编码为 MP4 视频文件）
 * 3. 获取截图（支持整个页面或指定元素）
 */
@Slf4j
public class HeadlessPageWrapper {

    private final BrowserContext context;
    private final Page page;

    /** 录制帧率（每秒帧数） */
    private static final int RECORDING_FPS = 5;

    /** 录制截图间隔（毫秒） */
    private static final int RECORDING_INTERVAL_MS = 1000 / RECORDING_FPS;

    /** 是否正在录制 */
    private final AtomicBoolean recording = new AtomicBoolean(false);

    /** 是否已关闭（由 HeadlessBrowserSession.close() 设置） */
    private volatile boolean closed = false;

    /** 录制帧缓冲区（内存中保存 PNG 字节） */
    private final List<byte[]> frameBuffer = Collections.synchronizedList(new ArrayList<>());

    /** 定时截图调度器 */
    private ScheduledExecutorService scheduler;

    HeadlessPageWrapper(BrowserContext context, Page page) {
        this.context = context;
        this.page = page;
    }

    /**
     * 标记为已关闭，由 HeadlessBrowserSession.close() 调用
     */
    void markClosed() {
        closed = true;
        // 停止录制调度器（如果正在录制）
        if (recording.get()) {
            stopScheduler();
        }
    }

    /**
     * 检查底层 Playwright 资源是否仍然可用
     */
    private boolean isTargetAlive() {
        if (closed) return false;
        try {
            if (page != null && page.isClosed()) return false;
        } catch (com.microsoft.playwright.impl.TargetClosedError e) {
            return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // ==================== 屏幕录制 ====================

    /**
     * 开始屏幕录制
     * <p>
     * 基于定时 page.screenshot() 实现，截图受 BrowserContext 的 deviceScaleFactor 影响，
     * 设为 2.0 时可获取 Retina 级别的清晰截图。
     * 调用 {@link #stopRecording()} 后会将截图帧序列编码为 MP4 视频文件。
     * <p>
     * 注意：同一时间只能有一个录制会话，重复调用会被忽略。
     */
    public void startRecording() {
        if (!recording.compareAndSet(false, true)) {
            log.warn("录制已在进行中，忽略重复调用");
            return;
        }
        if (!isTargetAlive()) {
            recording.set(false);
            log.warn("浏览器已关闭，无法开始屏幕录制");
            return;
        }

        frameBuffer.clear();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "headless-recording");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            if (!isTargetAlive() || !recording.get()) {
                return;
            }
            try {
                byte[] screenshotBytes = page.screenshot();
                frameBuffer.add(screenshotBytes);
            } catch (com.microsoft.playwright.impl.TargetClosedError e) {
                log.warn("录制截图时浏览器已关闭: {}", e.getMessage());
            } catch (Exception e) {
                log.debug("录制截图失败: {}", e.getMessage());
            }
        }, 0, RECORDING_INTERVAL_MS, TimeUnit.MILLISECONDS);

        log.info("屏幕录制已开始 (定时截图方案, fps={}, interval={}ms)", RECORDING_FPS, RECORDING_INTERVAL_MS);
    }

    /**
     * 结束屏幕录制并生成 MP4 视频文件
     * <p>
     * 停止定时截图，将内存中的截图帧序列使用 JCodec 编码为 MP4 视频文件。
     * 调用方负责在合适时机上传该文件或使用完毕后删除。
     *
     * @return MP4 视频文件，没有正在录制的会话时返回 null
     */
    public File stopRecording() {
        if (!recording.compareAndSet(true, false)) {
            log.warn("没有正在进行的录制，忽略调用");
            return null;
        }

        // 停止调度器
        stopScheduler();

        List<byte[]> frames = new ArrayList<>(frameBuffer);
        frameBuffer.clear();

        if (frames.isEmpty()) {
            log.warn("录制期间未捕获到任何截图帧");
            return null;
        }

        log.info("屏幕录制已停止，共捕获 {} 帧", frames.size());

        try {
            Path mp4File = Files.createTempFile("headless-recording-", ".mp4");
            encodeToMp4(frames, mp4File);
            log.info("MP4 视频已生成: {}，大小: {} bytes", mp4File, Files.size(mp4File));
            return mp4File.toFile();
        } catch (Exception e) {
            log.error("结束屏幕录制失败", e);
            throw new RuntimeException("结束屏幕录制失败", e);
        }
    }

    /**
     * 停止录制并丢弃所有截图数据
     * <p>
     * 停止定时截图调度器，清空内存中的所有帧数据，不生成视频文件。
     * 适用于录制中途放弃、不需要保存视频的场景。
     */
    public void discardRecording() {
        if (!recording.compareAndSet(true, false)) {
            log.warn("没有正在进行的录制，忽略调用");
            return;
        }

        stopScheduler();
        int discarded = frameBuffer.size();
        frameBuffer.clear();
        log.info("录制已丢弃，共丢弃 {} 帧截图数据", discarded);
    }

    /**
     * 是否正在录制
     */
    public boolean isRecording() {
        return recording.get();
    }

    /**
     * 停止调度器并等待完成
     */
    private void stopScheduler() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
    }

    // ==================== 截图 ====================

    /**
     * 获取整个浏览器页面的截图
     *
     * @return PNG 格式的截图文件
     */
    public File screenshot() {
        return screenshot(null);
    }

    /**
     * 获取指定元素的截图
     * <p>
     * 通过 CSS 选择器定位元素，截取该元素渲染后的样式截图（包含所有 CSS 效果）。
     * 如果选择器为 null 或空，则截取整个页面。
     *
     * @param selector CSS 选择器，如 "#app"、".chat-container"、"[data-testid='xxx']"
     * @return PNG 格式的截图文件
     */
    public File screenshot(String selector) {
        if (!isTargetAlive()) {
            log.warn("浏览器已关闭，无法获取截图");
            return null;
        }
        try {
            byte[] bytes;
            if (selector != null && !selector.isEmpty()) {
                Locator locator = page.locator(selector);
                bytes = locator.screenshot(new Locator.ScreenshotOptions());
            } else {
                bytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            }
            Path tempFile = Files.createTempFile("headless-screenshot-", ".png");
            Files.write(tempFile, bytes);
            return tempFile.toFile();
        } catch (Exception e) {
            log.error("获取截图失败: selector={}", selector, e);
            throw new RuntimeException("获取截图失败", e);
        }
    }

    // ==================== 文件上传 ====================

    /**
     * 上传文件到文件服务
     * <p>
     * 通过 {@link FileUtils} 将文件上传，返回文件 ID。
     * 上传完成后自动删除本地文件。
     *
     * @param file 要上传的文件（如 screenshot 或 stopRecording 返回的文件）
     * @return 上传后的文件 ID
     */
    public String upload(File file) {
        return upload(file, buildProperty());
    }

    /**
     * 上传文件到文件服务（自定义属性）
     *
     * @param file     要上传的文件
     * @param property 文件属性（有效期、权限等）
     * @return 上传后的文件 ID
     */
    public String upload(File file, FileProperty property) {
        try {
            KitFileInfoVO fileInfo = FileUtils.upload(file, property);
            String fileId = fileInfo.getFileId();
            log.info("文件已上传，fileId={}, fileName={}", fileId, file.getName());
            return fileId;
        } catch (Exception e) {
            log.error("文件上传失败: {}", file.getName(), e);
            throw new RuntimeException("文件上传失败", e);
        } finally {
            deleteQuietly(file.toPath());
        }
    }

    // ==================== 内部工具方法 ====================

    /**
     * 构建默认文件属性：公共权限、15天有效期
     */
    private FileProperty buildProperty() {
        return FileProperty.builder()
                .scopePublic()
                .expiredTime(LocalDateTime.now().plusDays(15))
                .build();
    }

    /**
     * 将截图帧序列（内存中的 PNG 字节）编码为 MP4 视频
     */
    private void encodeToMp4(List<byte[]> frames, Path output) throws IOException {
        SeekableByteChannel channel = NIOUtils.writableFileChannel(output.toString());
        AWTSequenceEncoder encoder = null;
        try {
            encoder = AWTSequenceEncoder.createSequenceEncoder(output.toFile(), RECORDING_FPS);
            for (byte[] frameData : frames) {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(frameData));
                if (image != null) {
                    encoder.encodeImage(image);
                }
            }
            encoder.finish();
        } catch (Exception e) {
            log.error("视频编码异常", e);
            throw new IOException("视频编码异常", e);
        } finally {
            NIOUtils.closeQuietly(channel);
        }
    }

    /**
     * 静默删除文件
     */
    private void deleteQuietly(Path path) {
        if (path != null) {
            try { Files.deleteIfExists(path); } catch (IOException e) { log.debug("清理临时文件失败", e); }
        }
    }
}
