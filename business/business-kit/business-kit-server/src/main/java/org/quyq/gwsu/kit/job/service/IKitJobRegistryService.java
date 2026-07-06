package org.quyq.gwsu.kit.job.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.kit.job.domain.KitJobRegistry;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行器注册服务接口
 */
public interface IKitJobRegistryService extends IService<KitJobRegistry> {

    /**
     * 查询失效的注册ID（时间计算在 Java 中完成，消除 DATE_ADD/INTERVAL 方言差异）
     *
     * @param timeout 超时秒数
     * @param nowTime 当前时间
     * @return 失效注册ID列表
     */
    List<String> findDead(int timeout, LocalDateTime nowTime);

    /**
     * 查询所有有效的注册信息（时间计算在 Java 中完成）
     *
     * @param timeout 超时秒数
     * @param nowTime 当前时间
     * @return 有效注册列表
     */
    List<KitJobRegistry> findAll(int timeout, LocalDateTime nowTime);

    /**
     * 注册保存或更新（先查后决定 insert/update，消除 ON DUPLICATE KEY / ON CONFLICT 方言差异）
     *
     * @param id             主键ID
     * @param registryGroup  注册分组
     * @param registryKey    注册标识
     * @param registryValue  注册值
     * @param modifyTime     修改时间
     */
    void registrySaveOrUpdate(String id, String registryGroup, String registryKey, String registryValue, LocalDateTime modifyTime);

}
