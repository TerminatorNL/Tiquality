package cf.terminator.tiquality.store;

import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.util.Constants;
import cf.terminator.tiquality.util.FiFoQueue;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Random;

@SuppressWarnings("WeakerAccess")
public class PlayerTracker {

    private final GameProfile profile;

    protected long tick_time_remaining_ns = Constants.NS_IN_TICK_LONG;
    protected FiFoQueue<TiqualitySimpleTickable> untickedTickables = new FiFoQueue<>();

    /**
     * Creates a new playertracker using the supplied GameProfile.
     * @param profile the GameProfile of the owner.
     */
    public PlayerTracker(@Nonnull GameProfile profile) {
        this.profile = profile;
    }

    /**
     * Resets every tick with a granted number of tick time set by TiqualityCommand
     * Is initialized with time for a full tick. (Loading blocks mid-tick, or something like that)
     */
    public void setNextTickTime(long granted_ns){
        tick_time_remaining_ns = granted_ns;
    }

    /**
     * Checks if the owner of a block is a fake owner.
     * Trackers belonging to fake owners are not removed and kept in memory.
     * This method is meant to be overridden.
     *
     * @return true if this is a fake owner.
     */
    public boolean isFakeOwner(){
        return false;
    }


    /**
     * Returns true if this PlayerTracker requires Ticks to be assigned to it.
     * This is meant to be overriden, for fake player implementations.
     *
     * @return true if this is a ticking PlayerTracker. false if you do not want
     * the blocks of this owner to tick.
     */
    public boolean isConsumer(){
        return true;
    }

    /**
     * Checks if the owner of this tracker is online or not.
     * @param onlinePlayerProfiles an array of online players
     * @return true if online
     */
    public boolean isPlayerOnline(final GameProfile[] onlinePlayerProfiles){
        for(GameProfile profile : onlinePlayerProfiles){
            if(this.profile.equals(profile)){
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the tick time multiplier for the PlayerTracker.
     * This is used to distribute tick time in a more controlled manner.
     * @param cache The current online player cache
     * @return the multiplier
     */
    public double getMultiplier(final GameProfile[] cache){
        if(isPlayerOnline(cache)){
            return 1;
        }else{
            return TiqualityConfig.OFFLINE_PLAYER_TICK_TIME_MULTIPLIER;
        }
    }

    /**
     * Gets the owner corresponding to this PlayerTracker.
     * @return the owner's profile
     */
    public GameProfile getOwner(){
        return profile;
    }

    /**
     * Decreases the remaining tick time for a player.
     * @param time in nanoseconds
     */
    public void consume(long time){
        tick_time_remaining_ns -= time;
    }

    /**
     * Gets the remaining tick time this player has.
     * Can be compared against the set tick time to
     * check if there are any active ticking entities.
     *
     * @return the remaining tick time, in nanoseconds.
     */
    public long getRemainingTime(){
        return tick_time_remaining_ns;
    }

    /**
     * Updates the queued items first.
     * @return true if everything was updated, and there is more time left.
     */
    public boolean updateOld(){
        while(untickedTickables.size() > 0 && tick_time_remaining_ns >= 0) {
            long start = System.nanoTime();
            untickedTickables.take().doUpdateTick();
            consume(System.nanoTime() - start);
        }
        return tick_time_remaining_ns >= 0;
    }

    /**
     * Decides whether or not to tick, based on
     * the entities the player has already ticked.
     * @param tickable the tickable
     */
    public void tickTileEntity(ITickable tickable){
        if (updateOld() == false){
            /* If we run out of time, we add the entity to the list if its not already there.*/
            if (untickedTickables.containsRef((TiqualitySimpleTickable) tickable) == false) {
                untickedTickables.addToQueue((TiqualitySimpleTickable) tickable);

                //TileEntity e = (TileEntity) tickable;
                //ServerSideEvents.showBlocked(e.getWorld(), e.getPos());
            }
        }else{
            /* We still have time, lets do this reloadFromFile!*/
            long start = System.nanoTime();
            tickable.update();
            consume(System.nanoTime() - start);
        }
    }

    /**
     * Performs block tick if it can, if not, it will queue it for later.
     */
    public void doBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        if(updateOld() == false){
            /* If we run out of time, we add the entity to the list if its not already there.*/
            BlockUpdateHolder holder = new BlockUpdateHolder(block, world, pos, state, rand);
            if (untickedTickables.contains(holder) == false) {
                untickedTickables.addToQueue(holder);

                //ServerSideEvents.showBlocked(world, pos);
            }
        }else{
            /* We still have time, lets do this reloadFromFile!*/
            long start = System.nanoTime();
            block.updateTick(world, pos, state, rand);
            consume(System.nanoTime() - start);
        }
    }

    /**
     * Performs block tick if it can, if not, it will queue it for later.
     */
    public void doRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        if(updateOld() == false){
            /* If we run out of time, we add the entity to the list if its not already there. */
            BlockRandomUpdateHolder holder = new BlockRandomUpdateHolder(block, world, pos, state, rand);
            if (untickedTickables.contains(holder) == false) {
                untickedTickables.addToQueue(holder);



                //ServerSideEvents.showBlocked(world, pos);
            }
        }else{
            /* We still have time, lets do this reloadFromFile!*/
            long start = System.nanoTime();
            block.randomTick(world, pos, state, rand);
            consume(System.nanoTime() - start);
        }
    }

    /**
     * After running out of tick time for this player, the server may have more
     * tick time to spare after ticking other players, it grants unchecked ticks
     */
    public void grantTick(){
        if(untickedTickables.size() > 0) {
            untickedTickables.take().doUpdateTick();
        }
    }

    /**
     * @return true if the PlayerTracker needs more time to complete all of it's work.
     */
    public boolean isDone(){
        return untickedTickables.size() > 0;
    }

    /**
     * Debugging method. Do not use in production environments.
     * @return description
     */
    public String toString(){
        return "PlayerTracker:{Owner: '" + getOwner().getName() + "', nsleft: " + tick_time_remaining_ns + ", unticked: " + untickedTickables.size() + "}";
    }

    @Override
    public boolean equals(Object o){
        if(o == null || o instanceof PlayerTracker == false){
            return false;
        }else{
            return o == this || this.getOwner().getId().equals(((PlayerTracker) o).getOwner().getId());
        }
    }

    @Override
    public int hashCode(){
        return getOwner().getId().hashCode();
    }
}
