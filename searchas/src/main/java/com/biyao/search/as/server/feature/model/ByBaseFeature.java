package com.biyao.search.as.server.feature.model;

import java.io.Serializable;
import java.util.*;

/**
 * @author zj
 * @version 1.0
 * @date 2019/11/14 16:18
 * @description
 */
public class ByBaseFeature implements Map<String, String>, Cloneable, Serializable {

    private static final long         serialVersionUID         = 1L;
    private static final int          DEFAULT_INITIAL_CAPACITY = 16;

    private final Map<String, String> map;

    public ByBaseFeature(){
        this(DEFAULT_INITIAL_CAPACITY, false);
    }

    public ByBaseFeature(Map<String, String> map){
        this.map = map;
    }

    public ByBaseFeature(boolean ordered){
        this(DEFAULT_INITIAL_CAPACITY, ordered);
    }

    public ByBaseFeature(int initialCapacity){
        this(initialCapacity, false);
    }

    public ByBaseFeature(int initialCapacity, boolean ordered){
        if (ordered) {
            map = new LinkedHashMap<String, String>(initialCapacity);
        } else {
            map = new HashMap<String, String>(initialCapacity);
        }
    }


    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public String get(Object key) {
        return map.get(key);
    }

    @Override
    public String put(String key, String value) {
        return map.put(key, value);
    }

    @Override
    public String remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<String> values() {
        return map.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return map.entrySet();
    }

    @Override
    public ByBaseFeature clone(){
        return new ByBaseFeature(map instanceof LinkedHashMap
                ? new LinkedHashMap<String, String>(map)
                : new HashMap<String, String>(map)
        );
    }

    @Override
    public boolean equals(Object obj) {
        return this.map.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.map.hashCode();
    }

    /**
     * 流式put
     * @param key
     * @param value
     * @return
     */
    public ByBaseFeature fluentPut(String key, String value) {
        map.put(key, value);
        return this;
    }
}