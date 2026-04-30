package org.quyq.gwsu.common.security.casbin;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/4
 * @description
 */
@RequiredArgsConstructor
public class RedisAdapter implements Adapter {

    private final CacheUtils cacheUtils;


    @Override
    public void loadPolicy(Model model) {
        String policies = cacheUtils.withRebel(() -> cacheUtils.get(SecurityConstants.Abac.PERMISSION_DATA_CACHE_KEY));
        if (!StringUtils.hasText(policies)) {
            return;
        }

        for (List<String> policy : new Gson().fromJson(policies, new TypeToken<List<List<String>>>() {
        })) {
            String pType = policy.get(0);  // "p"
            List<String> rule = policy.subList(1, policy.size());
            model.addPolicy("p", pType, rule);
        }

    }

    @Override
    public void savePolicy(Model model) {
        throw new UnsupportedOperationException("Not supported savePolicy.");
    }

    @Override
    public void addPolicy(String s, String s1, List<String> list) {
        throw new UnsupportedOperationException("Not supported addPolicy.");
    }

    @Override
    public void removePolicy(String s, String s1, List<String> list) {
        throw new UnsupportedOperationException("Not supported removePolicy.");
    }

    @Override
    public void removeFilteredPolicy(String s, String s1, int i, String... strings) {
        throw new UnsupportedOperationException("Not supported removeFilteredPolicy.");
    }
}
