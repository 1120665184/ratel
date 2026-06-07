package org.quyq.gwsu.kit.file.service;

import cn.hutool.crypto.digest.MD5;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.quyq.gwsu.kit.config.properties.FileUploadInfoProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.file.service.impl.CosFileServiceImpl;
import org.quyq.gwsu.kit.file.service.impl.LocalFileServiceImpl;
import org.quyq.gwsu.kit.file.service.impl.MinioFileServiceImpl;
import org.quyq.gwsu.kit.file.service.impl.OssFileServiceImpl;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileServiceManager {

    private final GenericApplicationContext context;
    private final ObjectMapper objectMapper;

    public static final String FILE_SYSTEM_CONFIG = "upload_server_info_config";

    // 缓存：配置MD5 -> 服务实例（Spring代理对象，支持AOP）
    private final Map<String, IFileService> serviceCache = new ConcurrentHashMap<>();
    // 当前正在使用的配置MD5（用于检测变更并清理旧的）
    private volatile String currentMd5 = null;

    /**
     * 获取当前启用的文件服务
     *
     * @return
     */
    public IFileService get() {
        FileUploadInfoProperties config = ConfigInfoUtils.getByObject(FILE_SYSTEM_CONFIG, FileUploadInfoProperties.class);

        if (config.getType() == null) {
            throw new BusinessException(KitErrorCode.E01008, "文件服务类型未配置");
        }

        String newMd5 = genMd5(config);

        // 配置变更：清理旧的缓存和Spring Bean
        if (currentMd5 != null && !currentMd5.equals(newMd5)) {
            IFileService oldService = serviceCache.remove(currentMd5);
            if (oldService != null) {
                destroyBean("fileService_" + currentMd5);
                log.info("配置变更，销毁旧文件服务，oldMd5={}, newMd5={}", currentMd5, newMd5);
            }
        }

        // 尝试从缓存获取（双重检查锁）
        IFileService service = serviceCache.get(newMd5);
        if (service != null) {
            currentMd5 = newMd5;
            return service;
        }

        synchronized (this) {
            service = serviceCache.get(newMd5);
            if (service != null) {
                currentMd5 = newMd5;
                return service;
            }

            // 注册新Bean
            service = registerBean(config, newMd5);
            serviceCache.put(newMd5, service);
            currentMd5 = newMd5;
            log.info("动态注册文件服务成功，type={}, md5={}", config.getType(), newMd5);
            return service;
        }
    }

    private IFileService registerBean(FileUploadInfoProperties config, String md5) {
        IFileService target = switch (config.getType()) {
            case MINIO -> new MinioFileServiceImpl(config);
            case LOCAL -> new LocalFileServiceImpl(config);
            case COS -> new CosFileServiceImpl(config, objectMapper);
            case OSS -> new OssFileServiceImpl(config, objectMapper);
        };

        String beanName = "fileService_" + md5;

        // 防御：如果Bean已存在，先销毁（正常情况下不会发生）
        if (context.containsBean(beanName)) {
            destroyBean(beanName);
        }

        context.registerBean(beanName, IFileService.class, () -> target,
                bd -> bd.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON));

        // 返回Spring容器中的代理对象（确保事务等AOP生效）
        return context.getBean(beanName, IFileService.class);
    }

    private void destroyBean(String beanName) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
        if (beanFactory.containsBean(beanName)) {
            beanFactory.destroySingleton(beanName);
            beanFactory.removeBeanDefinition(beanName);
        }
    }

    /**
     * 主动销毁指定MD5对应的服务（可用于外部配置刷新监听）
     */
    public void destroyService(String md5) {
        IFileService service = serviceCache.remove(md5);
        if (service != null) {
            destroyBean("fileService_" + md5);
            if (md5.equals(currentMd5)) {
                currentMd5 = null;
            }
            log.info("主动销毁文件服务，md5={}", md5);
        }
    }

    private String genMd5(FileUploadInfoProperties config) {
        Object target = switch (config.getType()) {
            case MINIO -> config.getMinio();
            case OSS -> config.getOss();
            case COS -> config.getCos();
            case LOCAL -> config.getLocal();
        };
        try {
            String json = objectMapper.writeValueAsString(target);
            return MD5.create().digestHex(json);
        } catch (Exception e) {
            throw new BusinessException(KitErrorCode.E01001, "生成配置MD5失败", e);
        }
    }
}