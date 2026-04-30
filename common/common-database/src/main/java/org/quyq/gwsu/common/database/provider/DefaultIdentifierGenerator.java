package org.quyq.gwsu.common.database.provider;


import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;

/**
 * @author Quyq
 * @date 2026/4/19
 * @description
 */
public class DefaultIdentifierGenerator implements IdentifierGenerator {
    @Override
    public Number nextId(Object entity) {
        return IdUtil.getSnowflakeNextId();
    }
}
