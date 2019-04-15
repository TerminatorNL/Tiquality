package cf.terminator.tiquality.tracking.tickqueue;

import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.tracking.TickLogger;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RelativeTPSTracker implements TiqualitySimpleTickable {

    private final TickQueue queue;
    private int worldTicks = 0;
    private int actualTrackerTicks = 0;
    private double ratio = 1;
    private boolean marked = false;

    RelativeTPSTracker(TickQueue queue){
        this.queue = queue;
        this.queue.addToQueue(this);
    }

    public void reset(){
        worldTicks = 0;
        actualTrackerTicks = 0;
    }

    /**
     * Gets the ratio of tick time compared to server tick time.
     * the returned value is always between 0 and 1.
     *
     * @return A value of 1 would indicate that this tracker is running at the same speed as the server.
     */
    public double getRatio(){
        synchronized (this) {
            return ratio;
        }
    }

    public void setRatio(double ratio){
        synchronized (this) {
            this.ratio = ratio;
        }
        if (ratio <= TiqualityConfig.DEFAULT_THROTTLE_WARNING_LEVEL) {
            Tracker tracker = this.queue.tracker.get();
            if (tracker != null) {
                tracker.notifyFallingBehind(ratio);
            }
        }
    }

    public void notifyNextTick(){
        worldTicks++;
        if (marked == false) {
            queue.addToQueue(this);
        }
        if (worldTicks % 100 == 0){
            double ratio_raw = (double) actualTrackerTicks / Math.max((double) worldTicks,1);
            setRatio(ratio_raw);
            reset();
        }
    }

    @Override
    public void doUpdateTick() {
        actualTrackerTicks++;
    }

    @Override
    public BlockPos getPos() {
        return null;
    }

    @Override
    public World getWorld() {
        return null;
    }

    @Override
    public TickLogger.Location getLocation() {
        return null;
    }

    @Override
    public TickType getType() {
        return TickType.OTHER;
    }

    @Override
    public void tiquality_mark() {
        marked = true;
    }

    @Override
    public void tiquality_unMark() {
        marked = false;
    }

    @Override
    public boolean tiquality_isMarked() {
        return marked;
    }
}
