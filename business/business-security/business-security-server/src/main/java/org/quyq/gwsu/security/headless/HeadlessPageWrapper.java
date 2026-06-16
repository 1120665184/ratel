package org.quyq.gwsu.security.headless;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import lombok.extern.slf4j.Slf4j;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.quyq.gwsu.kit.api.file.dto.FileProperty;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.api.utils.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipFile;

/**
 * 无头浏览器页面操作包装器
 * <p>
 * 封装 BrowserContext 和 Page 的高级操作，在 HeadlessAgentListener 事件回调中暴露，
 * 供调用方在事件处理过程中对浏览器界面进行操作（截图、录制等）。
 * <p>
 * 功能：
 * 1. 开始屏幕录制（基于 Playwright Tracing，支持动态启停）
 * 2. 结束屏幕录制（将 trace 中的截图序列编码为 MP4 视频文件）
 * 3. 获取截图（支持整个页面或指定元素）
 */
@Slf4j
public class HeadlessPageWrapper {

    private final BrowserContext context;
    private final Page page;

    /** 录制帧率（每秒帧数），Playwright Tracing 默认约每 200ms 截一帧 */
    private static final int RECORDING_FPS = 5;

    /** 是否正在录制 */
    private final AtomicBoolean recording = new AtomicBoolean(false);

    HeadlessPageWrapper(BrowserContext context, Page page) {
        this.context = context;
        this.page = page;
    }

    // ==================== 屏幕录制 ====================

    /**
     * 开始屏幕录制
     * <p>
     * 基于 Playwright Tracing 实现，捕获操作过程中的截图和 DOM 快照。
     * 调用 {@link #stopRecording()} 后会将截图序列编码为 MP4 视频文件。
     * <p>
     * 注意：同一时间只能有一个录制会话，重复调用会被忽略。
     */
    public void startRecording() {
        if (!recording.compareAndSet(false, true)) {
            log.warn("录制已在进行中，忽略重复调用");
            return;
        }
        try {
            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true));
            log.info("屏幕录制已开始");
        } catch (Exception e) {
            recording.set(false);
            log.error("开始屏幕录制失败", e);
            throw new RuntimeException("开始屏幕录制失败", e);
        }
    }

    /**
     * 结束屏幕录制并生成 MP4 视频文件
     * <p>
     * 停止录制，从 trace zip 中提取截图序列，使用 JCodec 编码为 MP4 视频文件。
     * 调用方负责在合适时机上传该文件或使用完毕后删除。
     *
     * @return MP4 视频文件，没有正在录制的会话时返回 null
     */
    public File stopRecording() {
        if (!recording.compareAndSet(true, false)) {
            log.warn("没有正在进行的录制，忽略调用");
            return null;
        }

        Path traceZip = null;
        Path traceDir = null;
        try {
            // 1. 停止录制并保存 trace zip
            traceZip = Files.createTempFile("headless-trace-", ".zip");
            context.tracing().stop(new Tracing.StopOptions().setPath(traceZip));
            log.info("屏幕录制已停止，trace 文件大小: {} bytes", Files.size(traceZip));

            // 2. 解压 trace zip
            traceDir = Files.createTempDirectory("headless-trace-");
            unzip(traceZip, traceDir);

            // 3. 提取 resources 目录下的截图文件，按文件名时间戳排序
            Path resourcesDir = traceDir.resolve("resources");
            if (!Files.isDirectory(resourcesDir)) {
                log.warn("trace zip 中未找到 resources 目录，无法生成视频");
                return null;
            }

            List<Path> frames = Files.list(resourcesDir)
                    .filter(p -> p.toString().endsWith(".jpeg") || p.toString().endsWith(".png"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();

            if (frames.isEmpty()) {
                log.warn("trace zip 中未找到截图文件，无法生成视频");
                return null;
            }

            log.info("从 trace 中提取到 {} 帧截图", frames.size());

            // 4. 使用 JCodec 编码为 MP4
            Path mp4File = Files.createTempFile("headless-recording-", ".mp4");
            encodeToMp4(frames, mp4File);
            log.info("MP4 视频已生成: {}，大小: {} bytes", mp4File, Files.size(mp4File));

            return mp4File.toFile();
        } catch (Exception e) {
            log.error("结束屏幕录制失败", e);
            throw new RuntimeException("结束屏幕录制失败", e);
        } finally {
            // 5. 清理临时 trace 文件和解压目录
            deleteQuietly(traceZip);
            deleteDirQuietly(traceDir);
        }
    }

    /**
     * 是否正在录制
     */
    public boolean isRecording() {
        return recording.get();
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
     * 解压 zip 文件到目标目录
     */
    private void unzip(Path zipPath, Path targetDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            zipFile.stream().forEach(entry -> {
                try {
                    Path entryPath = targetDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        Files.copy(zipFile.getInputStream(entry), entryPath);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("解压 trace zip 失败: " + entry.getName(), e);
                }
            });
        }
    }

    /**
     * 将截图序列编码为 MP4 视频
     */
    private void encodeToMp4(List<Path> frames, Path output) throws IOException {
        SeekableByteChannel channel = NIOUtils.writableFileChannel(output.toString());
        AWTSequenceEncoder encoder = null;
        try {
            encoder = AWTSequenceEncoder.createSequenceEncoder(output.toFile(), RECORDING_FPS);
            for (Path frame : frames) {
                BufferedImage image = ImageIO.read(frame.toFile());
                if (image != null) {
                    encoder.encodeImage(image);
                }
            }
            encoder.finish();
        }catch (Exception e) {
            log.info("转换异常："  ,e);
        }
        finally {
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

    /**
     * 静默删除目录及内容
     */
    private void deleteDirQuietly(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.debug("清理临时目录失败", e);
        }
    }
}
