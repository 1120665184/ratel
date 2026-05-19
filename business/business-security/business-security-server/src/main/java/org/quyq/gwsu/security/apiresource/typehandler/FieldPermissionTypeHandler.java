package org.quyq.gwsu.security.apiresource.typehandler;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.quyq.gwsu.common.security.domain.FieldPermission;

/**
 * FieldPermission 单对象 JSON TypeHandler
 * 用于 SecurityTableModelColumn.fieldConfig 字段的数据库读写
 *
 * @author Quyq
 */
@MappedTypes({FieldPermission.class})
@MappedJdbcTypes({JdbcType.VARCHAR})
public class FieldPermissionTypeHandler extends AbstractJsonTypeHandler<FieldPermission> {

    public FieldPermissionTypeHandler(Class<?> type) {
        super(type);
    }

    @Override
    public FieldPermission parse(String json) {
        return Jackson3TypeHandler.getObjectMapper().readValue(json, FieldPermission.class);
    }

    @Override
    public String toJson(FieldPermission obj) {
        return Jackson3TypeHandler.getObjectMapper().writeValueAsString(obj);
    }
}
