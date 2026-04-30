package org.quyq.gwsu.common.security.dataresource;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.DataResoureRule;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据资源规则获取工具类
 * 从 Redis 获取数据资源过滤规则
 *
 * @author Quyq
 * @date 2026/4/20
 */
@RequiredArgsConstructor
public class DataResourceRuleUtils {

    private final CacheUtils cacheUtils;

    /**
     * 获取所有数据资源规则
     *
     * @return 数据资源规则列表
     */
    public List<DataResoureRule> getAllRules() {
        List<DataResoureRule> rules = cacheUtils.withRebel(() ->
                cacheUtils.get(SecurityConstants.DataResource.DATA_RESOURCE_RULES_CACHE_KEY)
        );

        return rules != null ? rules : Collections.emptyList();
    }

    /**
     * 获取按表名分组的规则映射
     *
     * @return 表名 -> 规则列表 的映射
     */
    public Map<String, List<DataResoureRule>> getRulesGroupByTable() {
        return getAllRules().stream()
                .collect(Collectors.groupingBy(DataResoureRule::getTableName));
    }

    /**
     * 根据表名获取对应的规则
     *
     * @param tableName 表名
     * @return 该表的规则列表
     */
    public List<DataResoureRule> getRulesByTableName(String tableName) {
        return getRulesGroupByTable().getOrDefault(tableName, Collections.emptyList());
    }

    /**
     * 根据表名和库名获取对应的规则
     *
     * @param databaseName 库名
     * @param tableName    表名
     * @return 匹配的规则列表
     */
    public List<DataResoureRule> getRules(String databaseName, String tableName) {
        return getAllRules().stream()
                .filter(rule -> tableName.equals(rule.getTableName()))
                .filter(rule -> rule.getDatabaseName() == null ||
                        databaseName == null ||
                        databaseName.equals(rule.getDatabaseName()))
                .toList();
    }

}
