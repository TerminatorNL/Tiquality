package cf.terminator.tiquality.util;

import java.util.Map;

public class Utils {

    public static <K,V> V removeKeyByValue(Map<K,V> map, V value){
        K key = null;
        for(Map.Entry<K,V> entry : map.entrySet()){
            if(value.equals(entry.getValue())){
                key = entry.getKey();
                break;
            }
        }
        return key == null ? null : map.remove(key);
    }

    public static <K,V> K findKeyByValue(Map<K,V> map, V value){
        K key = null;
        for(Map.Entry<K,V> entry : map.entrySet()){
            if(value.equals(entry.getValue())){
                key = entry.getKey();
                break;
            }
        }
        return key;
    }
}
