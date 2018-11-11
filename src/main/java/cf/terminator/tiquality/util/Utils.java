package cf.terminator.tiquality.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;

import java.util.AbstractMap;
import java.util.HashMap;
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

    public static Vec3d getLookVec(Entity entity, int distance){
        return entity.getPositionEyes(1F).add(entity.getLookVec().scale(distance));
    }

    private static final HashMap<EntityPlayer, Map.Entry<Long,ITextComponent>> LAST_MESSAGES = new HashMap<>();
    public static void sendStatusBarMessage(EntityPlayer player, ITextComponent text){
        if(text == null || player == null){
            return;
        }
        if(LAST_MESSAGES.containsKey(player) == false || LAST_MESSAGES.get(player).getValue().getFormattedText().equals(text.getFormattedText()) == false || LAST_MESSAGES.get(player).getKey() > System.currentTimeMillis() + 3000){
            LAST_MESSAGES.put(player, new AbstractMap.SimpleEntry<>(System.currentTimeMillis(), text));
            player.sendStatusMessage(text, true);
        }
    }
}
