package org.quyq.gwsu.common.core.domain;

import lombok.Data;

/**
 * @author Quyq
 * @date 2024/5/10
 * @description 简单的键值对实体对象
 */
@Data
public class KeyValue<K,V> {

    public KeyValue(){}

    public KeyValue(K key, V value) {
        this.key = key;
        this.value = value;
    }

    private K key;

    private V value;

}
