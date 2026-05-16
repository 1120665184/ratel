package org.quyq.gwsu.common.security.service.impl;


import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.quyq.gwsu.common.security.domain.vo.SqlQueryVO;
import org.quyq.gwsu.common.security.service.ISQLExecutionService;
import org.quyq.gwsu.common.security.utils.SqlDataPermissionFilterUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/5/16
 * @description
 */
@RequiredArgsConstructor
public class SQLExecutionServiceImpl implements ISQLExecutionService {

    private final JdbcTemplate jdbcTemplate;
    private final SqlDataPermissionFilterUtils sqlDataPermissionFilterUtils;

    @Override
    public SqlQueryVO query(String datasource, String sql, List<Object> parameters) {

        if (!StringUtils.hasText(sql)) {
            return null;
        }
        //添加当前用户的数据权限
        sql = sqlDataPermissionFilterUtils.applyDataPermission(sql);

        if (StringUtils.hasText(datasource)) {
            DynamicDataSourceContextHolder.push(datasource);
        }
        List<Map<String, @Nullable Object>> maps;
        try {
            if (Objects.isNull(parameters)) {
                maps = jdbcTemplate.queryForList(sql);
            } else {
                maps = jdbcTemplate.queryForList(sql, parameters.toArray());
            }
            return new SqlQueryVO(sql, maps);
        } finally {
            if (StringUtils.hasText(datasource)) {
                DynamicDataSourceContextHolder.clear();
            }
        }
    }
}
