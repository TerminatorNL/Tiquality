package cf.terminator.tiquality.api;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.util.SynchronizedAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
/**
 * Contains functions to get/set PlayerTrackers
 */
@SuppressWarnings("unused")
public class WorldData {

    /**
     * Get the PlayerTracker
     * @param world the world
     * @param pos the block position
     * @return the tracker, or null if no tracker is found.
     *
     * BE WARNED: If you're in another thread, AND the server thread is WAITING (blocked) on your current thread,
     * this will cause a deadlock!
     */
    public static @Nullable
    Tracker getTrackerAt(World world, BlockPos pos){
        return SynchronizedAction.run(new SynchronizedAction.Action<Tracker>() {
            @Override
            public void run(SynchronizedAction.DynamicVar<Tracker> variable) {
                variable.set(((TiqualityWorld) world).getTiqualityTracker(pos));
            }
        });
    }

    /**
     * Set the TrackerBase at a position in the world
     * @param world the world
     * @param pos the block position
     * @param tracker the tracker
     *
     * BE WARNED: If you're in another thread, AND the server thread is WAITING (blocked) on your current thread,
     * this will cause a deadlock!
     */
    public static void setTrackerAt(World world, BlockPos pos, Tracker tracker){
        Tiquality.SCHEDULER.scheduleWait(new Runnable() {
            @Override
            public void run() {
                ((TiqualityWorld) world).setTiqualityTracker(pos,tracker);
            }
        });
    }






}
