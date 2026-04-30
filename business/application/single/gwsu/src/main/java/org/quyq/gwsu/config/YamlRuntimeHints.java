package org.quyq.gwsu.config;


import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * @author Quyq
 * @date 2026/3/19
 * @description
 */
public class YamlRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        hints.resources().registerPattern("database.yaml");
        hints.resources().registerPattern("redis.yaml");
    }
}
