package org.quyq.gwsu.kit.file.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.file.mapper.KitFileInfoMapper;
import org.quyq.gwsu.kit.file.service.FileServiceManager;
import org.quyq.gwsu.kit.file.service.IFileService;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 过期文件定时清除任务
 * <p>
 * 每小时执行一次，查询 kit_file_info 中 expired_time 已过期的文件，
 * 调用 IFileService.remove() 完成数据库记录与物理文件的清理。
 * </p>
 *
 * @author Quyq
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredFileCleanupTask {

    private final FileServiceManager fileServiceManager;
    private final KitFileInfoMapper fileInfoMapper;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "expired-file-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.HOURS);
        log.info("过期文件定时清除任务已启动，执行间隔：1小时");
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            log.info("过期文件定时清除任务已停止");
        }
    }

    private void cleanup() {
        try {
            IFileService fileService = fileServiceManager.get();
            String serverType = fileService.getServerType().name();
            LocalDateTime now = LocalDateTime.now();

            List<KitFileInfoVO> expiredFiles = fileInfoMapper.getExpiredFilelist(now, serverType);

            if (expiredFiles == null || expiredFiles.isEmpty()) {
                log.debug("未发现过期文件");
                return;
            }

            log.info("发现 {} 个过期文件，开始清除...", expiredFiles.size());

            int successCount = 0;
            int failCount = 0;

            for (KitFileInfoVO fileInfo : expiredFiles) {
                try {
                    fileService.remove(fileInfo.getFileId());
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("清除过期文件失败，fileId={}, fileName={}", fileInfo.getFileId(), fileInfo.getFileName(), e);
                }
            }

            log.info("过期文件清除完成，成功: {}, 失败: {}", successCount, failCount);
        } catch (Exception e) {
            log.error("过期文件定时清除任务执行异常", e);
        }
    }
}
