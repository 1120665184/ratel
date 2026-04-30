package org.quyq.gwsu.common.database.typehandler;


import cn.hutool.core.util.NumberUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.util.StringUtils;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Quyq
 * @date 2026/4/16
 * @description
 */
public class BooleanTypeHandler extends BaseTypeHandler<Boolean> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, Boolean.TRUE.equals(parameter) ? 1 : 0);
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseBoolean(rs.getString(columnName));
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseBoolean(rs.getString(columnIndex));
    }

    @Override
    public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseBoolean(cs.getString(columnIndex));
    }

    private Boolean parseBoolean(String str) {
        if (!StringUtils.hasText(str)) {
            return null;
        }
        if (NumberUtil.isNumber(str)) {
            return Integer.parseInt(str) > 0;
        }
        return Boolean.parseBoolean(str);
    }

}
