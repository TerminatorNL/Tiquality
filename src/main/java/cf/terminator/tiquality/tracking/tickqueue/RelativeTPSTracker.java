package cf.terminator.tiquality.tracking.tickqueue;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.api.TiqualityException;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.profiling.ReferencedTickable;
import cf.terminator.tiquality.tracking.UpdateType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
    TODO: Detection between 100% and 50% speed must be made more accurate.
    I don't know how to tackle this properly yet.

    There's a queue that gets emptied FIFO style, and we have to monitor how fast it's doing it.
    But there's a caveat. The speed at which the queue is emptied varies, and you don't know the
    time the queue needs to finish without profiling (Profiling itself drops performance aswell).

    This implementation counts the amount of cycles a queue does by inserting itself over and over again.
    It cannot insert itself more than once a tick, because the queue will then think it has more work to do
    and will loop until it runs out of time, wasting CPU cycles.

 */
public class RelativeTPSTracker implements TiqualitySimpleTickable {

    private final TickQueue queue;
    private int worldTicks = 0;
    private int actualTrackerTicks = 0;
    private double tps = 20;
    private int mark_count = 0;
    private boolean tickedThisTick = false;
    private long startTime = System.currentTimeMillis();


    RelativeTPSTracker(TickQueue queue){
        this.queue = queue;
        this.queue.addToQueue(this);
    }

    public void reset(){
        worldTicks = 0;
        actualTrackerTicks = 0;
        startTime = System.currentTimeMillis();
    }

    /**
     * Gets the absolute TPS of this Queue.
     *
     * @return TPS value, raw. (Could be more than 20)
     */
    public double getTps(){
        synchronized (this) {
            return tps;
        }
    }

    public void setTPS(double tps){
        synchronized (this) {
            this.tps = tps;
        }
        double ratio = tps/Tiquality.TPS_MONITOR.getAverageTPS();
        if (ratio <= TiqualityConfig.DEFAULT_THROTTLE_WARNING_LEVEL) {
            Tracker tracker = this.queue.tracker.get();
            if (tracker != null) {
                tracker.notifyFallingBehind(ratio);
            }
        }
    }

    /**
     * Called when the server is about to tick the world.
     */
    public void notifyNextTick(){
        worldTicks++;
        tickedThisTick = false;
        if (mark_count == 0) {
            queue.addToQueue(this);
        }
        if (worldTicks % 100 == 0){
            long endTime = System.currentTimeMillis();
            double durationInSeconds = (endTime - startTime)/1000D;
            setTPS(actualTrackerTicks/durationInSeconds);
            reset();
        }
    }

    /**
     * Always true.
     */
    @Override
    public boolean tiquality_isLoaded() {
        return true;
    }

    /**
     * Called when the queue executes this task
     */
    @Override
    public void tiquality_doUpdateTick() {
        if (tickedThisTick == false) {
            actualTrackerTicks++;
            queue.addToQueue(this);
            Tracker tracker = queue.tracker.get();
            if(tracker != null && tracker.isProfiling()){
                try {
                    tracker.getTickLogger().addTrackerTick();
                } catch (TiqualityException.TrackerWasNotProfilingException e) {
                    //Should never happen
                    e.printStackTrace();
                }
            }
        }
        tickedThisTick = true;
    }

    @Override
    public BlockPos tiquality_getPos() {
        return null;
    }

    @Override
    public World tiquality_getWorld() {
        return null;
    }

    @Nullable
    @Override
    public ReferencedTickable.Reference getId() {
        return null;
    }

    /**
     * Called before the queue adds this task to itself
     */
    @Override
    public void tiquality_mark() {
        mark_count++;
    }

    /**
     * Called before the queue executes the task, when already
     * have taken the task out of the queue, but has not executed it yet
     */
    @Override
    public void tiquality_unMark() {
        mark_count--;
    }

    /**
     * Not called by anything in this instance.
     * @return marked
     */
    @Override
    public boolean tiquality_isMarked() {
        return mark_count > 0;
    }

    @Override
    public void setUpdateType(@Nonnull UpdateType type) {

    }

    @Nonnull
    @Override
    public UpdateType getUpdateType() {
        return UpdateType.DEFAULT;
    }
}
