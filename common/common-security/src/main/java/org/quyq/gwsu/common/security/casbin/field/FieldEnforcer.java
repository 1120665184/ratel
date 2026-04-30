package org.quyq.gwsu.common.security.casbin.field;


import com.googlecode.aviator.Expression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.exception.CasbinMatcherException;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.util.Util;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.FieldRule;
import org.quyq.gwsu.common.security.domain.RequestContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Quyq
 * @date 2026/4/5
 * @description 字段权限判断执行
 */
@RequiredArgsConstructor
@Slf4j
public class FieldEnforcer implements InitializingBean, DisposableBean {

    private final CacheUtils cacheUtils;

    private final Enforcer forcer;

    private RedisMessageListenerContainer listenerContainer;

    private final Map<String, List<FieldRule>> allRules = new HashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final AntPathMatcher matcher = new AntPathMatcher();


    /**
     * 获取匹配到当前上下文中的字段权限规则
     *
     * @param context
     * @return
     */
    public List<FieldRule> getMatchingRules(RequestContext context) {
        List<FieldRule> rules;

        lock.readLock().lock();
        try {
            rules = allRules.get("%s:%s".formatted(context.resType(), context.action()));
            if (CollectionUtils.isEmpty(rules)) {
                return Collections.emptyList();
            }
        } finally {
            lock.readLock().unlock();
        }


        return rules.stream().filter(r -> matcher.match(r.url(), context.resUrl()) && abacMatch(r, context)).toList();

    }


    private boolean abacMatch(FieldRule rule, RequestContext context) {
        Model model = forcer.getModel();
        String pType = "p";
        List<String> pVals = List
                .of("*",
                        context.resType(),
                        context.action(),
                        rule.url(),
                        rule.expression(),
                        rule.effect());
        String[] pTokens = model.model.get("p").get(pType).tokens;

        String rType = "r";
        List<Object> rVals = List.of(context.subject(), context.resType(), context.action(), context.resUrl(), context.env());

        String[] rTokens = model.model.get("r").get(rType).tokens;


        Map<String, Object> parameters = HashMap.newHashMap(rTokens.length + pTokens.length);
        putToken(parameters, pType, pVals, pTokens);
        putToken(parameters, rType, rVals, rTokens);

        String r = replaceTargets(Util.convertInSyntax(rule.expression()));

        Expression expression = forcer.getAviatorEval().compile(Util.md5(r), r, false);

        Object result = expression.execute(parameters);
        if (result instanceof Boolean bool) {
            return bool;
        } else if (result instanceof Double || result instanceof Long) {
            return ((Number) result).floatValue() != 0;
        }

        throw new CasbinMatcherException("matcher result should be Boolean, Double or Long");

    }

    private void putToken(Map<String, Object> parameters, String type, List<?> valus, String[] tokens) {
        if (tokens.length != valus.size()) {
            throw new CasbinMatcherException("invalid policy size: expected " + tokens.length +
                    ", got " + valus.size() + ", rvals or pvals: " + valus);
        }
        for (int i = 0; i < tokens.length; i++) {
            parameters.put(tokens[i], valus.get(i));
        }
    }

    private String replaceTargets(String exp) {
        //Replace the first dot, because it can't be recognized by the 'reg' below.
        if (exp.startsWith("r") || exp.startsWith("p")) {
            exp = exp.replaceFirst("\\.", "_");
        }
        //match example: "&&r.","||r."，"=r."
        String reg = "([| =)(&<>,+\\-*/!])((r|p)[0-9]*)\\.";
        exp = exp.replaceAll(reg, "$1$2_");
        return exp;
    }


    @Override
    public void afterPropertiesSet() {
        initRules();
        initListener();
    }

    /**
     * 初始化规则
     */
    private void initRules() {
        log.info("字段权限规则加载...");
        Map<String, List<FieldRule>> rules = cacheUtils.withRebel(() -> cacheUtils.get(SecurityConstants.Abac.PERMISSION_FIELD_CACHE_KEY));

        lock.writeLock().lock();
        try {
            allRules.clear();
            if (!CollectionUtils.isEmpty(rules)) {
                allRules.putAll(rules);
            }

        } finally {
            lock.writeLock().unlock();
        }


    }

    /**
     * 初始化监听器
     */
    private void initListener() {
        this.listenerContainer = cacheUtils.withRebel(() ->
                cacheUtils.addListener(SecurityConstants.Abac.PERMISSION_CHANGE_NOTICE_TOPIC,
                        (message, pattern) -> {
                            Object msg = cacheUtils.getSerializer().deserialize(message.getBody());
                            if (msg instanceof String val && "syncField".equals(val)) {
                                initRules();
                            }
                        })
        );
    }


    @Override
    public void destroy() {
        if (Objects.nonNull(this.listenerContainer)) {
            this.listenerContainer.stop();
        }
    }
}
