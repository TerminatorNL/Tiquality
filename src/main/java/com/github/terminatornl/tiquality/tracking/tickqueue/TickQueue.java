package com.github.terminatornl.tiquality.tracking.tickqueue;

import com.github.terminatornl.tiquality.interfaces.TiqualitySimpleTickable;
import com.github.terminatornl.tiquality.interfaces.TiqualityWorld;
import com.github.terminatornl.tiquality.interfaces.Tracker;
import com.github.terminatornl.tiquality.memory.WeakReferencedTracker;
import com.github.terminatornl.tiquality.tracking.UpdateType;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedList;

/**
 * Note: There is no actual implementation of contains on object level, but this is how this Queue is used.
 * <p>
 * This is a faster implementation of FiFoQueue, because now it no longer has to iterate through the list, because the
 * Tickables have a flag on them in either the object itself, or the world.
 */
public class TickQueue {

    public final WeakReferencedTracker tracker;
    public final RelativeTPSTracker relativeTPSTracker;
    private final LinkedList<TiqualitySimpleTickable> data = new LinkedList<>();

    public TickQueue(Tracker tracker) {
        this.tracker = new WeakReferencedTracker(tracker);
        this.relativeTPSTracker = new RelativeTPSTracker(this);
    }

    public void notifyNextTick() {
        relativeTPSTracker.notifyNextTick();
    }

    public boolean containsBlockUpdate(TiqualityWorld world, BlockPos pos) {
        return world.tiquality_isMarked(pos);
    }

    public boolean containsRandomBlockUpdate(TiqualityWorld world, BlockPos pos) {
        return world.tiquality_isMarked(pos);
    }

    public boolean containsTileEntityUpdate(TiqualitySimpleTickable tickable) {
        return tickable.tiquality_isMarked();
    }

    public boolean containsEntityUpdate(TiqualitySimpleTickable entity) {
        return entity.tiquality_isMarked();
    }

    public boolean containsSimpleUpdate(TiqualitySimpleTickable update) {
        return update.tiquality_isMarked();
    }

    /**
     * Add this tickable to the queue, which can be executed in the current tick.
     *
     * @param tickable the tickable
     */
    public void addToQueue(TiqualitySimpleTickable tickable) {
        tickable.tiquality_mark();
        if (tickable.getUpdateType() == UpdateType.PRIORITY) {
            data.addFirst(tickable);
        } else {
            data.addLast(tickable);
        }
    }

    public TiqualitySimpleTickable take() {
        TiqualitySimpleTickable t = data.removeFirst();
        t.tiquality_unMark();
        return t;
    }

    public void tickAll() {
        while (data.size() > 0) {
            TiqualitySimpleTickable tickable = take();
            if (tickable.tiquality_isLoaded()) {
                tickable.tiquality_doUpdateTick();
            }
        }
    }

    public int size() {
        return data.size();
    }
}
