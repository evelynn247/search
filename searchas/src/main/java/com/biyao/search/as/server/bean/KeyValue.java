package com.biyao.search.as.server.bean;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/11/21
 * K V结构的对象
 **/
public class KeyValue <K, V> {

    /**
     * key
     */
    private K key;
    /**
     * value
     */
    private V value;

    public K getKey(){
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }
}
