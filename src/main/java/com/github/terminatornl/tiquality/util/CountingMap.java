package com.github.terminatornl.tiquality.util;

import java.util.TreeMap;

public class CountingMap<K> extends TreeMap<K, Integer> {

    public CountingMap() {
        super();
    }

    public void addCount(K key, Integer value) {
        Integer v = super.get(key);
        v = v == null ? value : v + value;
        super.put(key, v);
    }
}
