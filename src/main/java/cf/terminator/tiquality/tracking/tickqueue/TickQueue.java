package cf.terminator.tiquality.tracking.tickqueue;

import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.memory.WeakReferencedTracker;
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

    private final LinkedList<TiqualitySimpleTickable> data = new LinkedList<>();
    public final WeakReferencedTracker tracker;
    public final RelativeTPSTracker relativeTPSTracker;

    public TickQueue(Tracker tracker){
        this.tracker = new WeakReferencedTracker(tracker);
        this.relativeTPSTracker = new RelativeTPSTracker(this);
    }

    public void notifyNextTick(){
        relativeTPSTracker.notifyNextTick();
    }

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

    public boolean containsSimpleUpdate(TiqualitySimpleTickable update){
        return update.tiquality_isMarked();
    }

    /**
     * Add this tickable to the queue, which can be executed in the current tick.
     * @param tickable the tickable
     */
    public void addToQueue(TiqualitySimpleTickable tickable){
        tickable.tiquality_mark();
        if(UPDATE_ITEMS_FIRST && tickable instanceof EntityItem){
            data.addFirst(tickable);
        }else{
            data.addLast(tickable);
        }
    }

    public TiqualitySimpleTickable take(){
        TiqualitySimpleTickable t = data.removeFirst();
        t.tiquality_unMark();
        return t;
    }

    public void tickAll(){
        while(data.size() > 0){
            take().doUpdateTick();
        }
    }

    public int size(){
        return data.size();
    }
}
