package cf.terminator.tiquality.util;

import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedList;

import static cf.terminator.tiquality.TiqualityConfig.UPDATE_ITEMS_FIRST;

/**
 * Note: There is no actual implementation of contains on object level, but this is how this Queue is used.
 *
 * This is a faster implementation of FiFoQueue, because now it no longer has to iterate through the list, because the
 * Tickables have a flag on them in either the object itself, or the world.
 */
public class TickQueue {

    private LinkedList<TiqualitySimpleTickable> data = new LinkedList<>();

    public boolean containsBlockUpdate(TiqualityWorld world, BlockPos pos){
        return world.tiquality_isMarked(pos);
    }

    public boolean containsRandomBlockUpdate(TiqualityWorld world, BlockPos pos){
        return world.tiquality_isMarked(pos);
    }

    public boolean containsTileEntityUpdate(TiqualitySimpleTickable tickable){
        return tickable.tiquality_isMarked();
    }

    public boolean containsEntityUpdate(TiqualitySimpleTickable entity){
        return entity.tiquality_isMarked();
    }

    public void addToQueue(TiqualitySimpleTickable obj){
        obj.tiquality_mark();
        if(UPDATE_ITEMS_FIRST && obj instanceof EntityItem){
            data.addFirst(obj);
        }else{
            data.addLast(obj);
        }
    }

    public TiqualitySimpleTickable take(){
        TiqualitySimpleTickable t = data.removeFirst();
        t.tiquality_unMark();
        return t;
    }

    public int size(){
        return data.size();
    }
}
