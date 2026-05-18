package org.quyq.gwsu.security.apiresource.typehandler;


import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import tools.jackson.core.type.TypeReference;

import java.util.Map;

/**
 * @author Quyq
 * @date 2026/5/18
 * @description
 */
@MappedTypes({Map.class})
@MappedJdbcTypes({JdbcType.VARCHAR})
public class FieldConfigTypeHandler extends AbstractJsonTypeHandler<Map<String, FieldPermission>> {


    public FieldConfigTypeHandler(Class<?> type) {
        super(type);
    }

    @Override
    public Map<String, FieldPermission> parse(String json) {
        return Jackson3TypeHandler.getObjectMapper().readValue(json, new TypeReference<>() {
        });
    }

    @Override
    public String toJson(Map<String, FieldPermission> obj) {
        return Jackson3TypeHandler.getObjectMapper().writeValueAsString(obj);
    }
}
