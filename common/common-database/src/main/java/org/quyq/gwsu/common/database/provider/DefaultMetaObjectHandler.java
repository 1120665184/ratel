package org.quyq.gwsu.common.database.provider;


import cn.hutool.core.lang.func.LambdaUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.reflection.MetaObject;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/4/19
 * @description
 */
@RequiredArgsConstructor
public class DefaultMetaObjectHandler implements MetaObjectHandler {

    private final SecurityUtils securityUtils;

    @Override
    public void insertFill(MetaObject metaObject) {
        String username = Objects.isNull(securityUtils) ? null : securityUtils.getUsername();
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, LambdaUtil.getFieldName(BaseDO::getCreateTime), LocalDateTime.class, now);
        this.strictInsertFill(metaObject, LambdaUtil.getFieldName(BaseDO::getModifyTime), LocalDateTime.class, now);
        if (StringUtils.hasText(username)) {
            this.strictInsertFill(metaObject, LambdaUtil.getFieldName(BaseDO::getCreateOp), String.class, username);
            this.strictInsertFill(metaObject, LambdaUtil.getFieldName(BaseDO::getModifyOp), String.class, username);
        }
        deleteValueSet(metaObject, username, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        String username = Objects.isNull(securityUtils) ? null :  securityUtils.getUsername();
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, LambdaUtil.getFieldName(BaseDO::getModifyTime), LocalDateTime.class, now);
        if (StringUtils.hasText(username)) {
            this.strictInsertFill(metaObject, LambdaUtil.getFieldName(BaseDO::getModifyOp), String.class, username);
        }
        deleteValueSet(metaObject, username, now);
    }


    private void deleteValueSet(MetaObject metaObject, String username, LocalDateTime now) {
        if (metaObject.getOriginalObject() instanceof BaseDO val) {
            if (Objects.nonNull(val.getDeleted()) && val.getDeleted()) {
                this.strictUpdateFill(metaObject, LambdaUtil.getFieldName(BaseDO::getDeleteOp), String.class, username);
                this.strictUpdateFill(metaObject, LambdaUtil.getFieldName(BaseDO::getDeleteTime), LocalDateTime.class, now);
            }
        }
    }


}
