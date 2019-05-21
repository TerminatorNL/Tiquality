package cf.terminator.tiquality.util;

import java.util.TreeMap;

public class CountingMap<K> extends TreeMap<K,Integer> {

    public CountingMap(){
        super();
    }

    public void addCount(K key, Integer value){
        Integer v = super.get(key);
        if(v == null){
            v = value;
        }else{
            v = v + value;
        }
        super.put(key, v);
    }
}
