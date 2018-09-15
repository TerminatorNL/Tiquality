package cf.terminator.tiquality.api;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.store.PlayerTracker;
import cf.terminator.tiquality.store.TrackerHub;
import cf.terminator.tiquality.util.SynchronizedAction;
import com.mojang.authlib.GameProfile;
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
     * @return the PlayerTracker, or null if now owner is found.
     *
     * BE WARNED: If you're in another thread, AND the server thread is WAITING (blocked) on your current thread,
     * this will cause a deadlock!
     */
    public static @Nullable PlayerTracker getPlayerTrackerAt(World world, BlockPos pos){
        return SynchronizedAction.run(new SynchronizedAction.Action<PlayerTracker>() {
            @Override
            public void run(SynchronizedAction.DynamicVar<PlayerTracker> variable) {
                variable.set(((TiqualityWorld) world).getPlayerTracker(pos));
            }
        });
    }

    /**
     * Set the PlayerTracker at a position in the world
     * @param world the world
     * @param pos the block position
     * @param profile the profile
     *
     * BE WARNED: If you're in another thread, AND the server thread is WAITING (blocked) on your current thread,
     * this will cause a deadlock!
     */
    public static void setPlayerTrackerAt(World world, BlockPos pos, GameProfile profile){
        PlayerTracker tracker = TrackerHub.getOrCreatePlayerTrackerByProfile(profile);
        Tiquality.SCHEDULER.scheduleWait(new Runnable() {
            @Override
            public void run() {
                ((TiqualityWorld) world).setPlayerTracker(pos,tracker);
            }
        });
    }






}
