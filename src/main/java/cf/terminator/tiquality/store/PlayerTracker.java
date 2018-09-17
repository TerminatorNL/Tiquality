package cf.terminator.tiquality.store;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.util.Constants;
import cf.terminator.tiquality.util.FiFoQueue;
import cf.terminator.tiquality.util.SynchronizedAction;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Random;

@SuppressWarnings("WeakerAccess")
public class PlayerTracker {

    private final GameProfile profile;

    protected long tick_time_remaining_ns = Constants.NS_IN_TICK_LONG;
    protected FiFoQueue<TiqualitySimpleTickable> untickedTickables = new FiFoQueue<>();
    protected HashSet<TiqualityChunk> ASSOCIATED_CHUNKS = new HashSet<>();
    protected TickLogger tickLogger = new TickLogger();

    /**
     * Internal use only. Used to determine when to unload.
     */
    private int unloadCooldown = 20;

    /**
     * Only changes between ticks
     */
    private boolean isProfiling = false;

    /**
     * Creates a new playertracker using the supplied GameProfile.
     * DO NOT USE THIS METHOD YOURSELF. See: cf.terminator.tiquality.store.TrackerHub
     *
     * @param profile the GameProfile of the owner.
     */
    PlayerTracker(@Nonnull GameProfile profile) {
        this.profile = profile;
    }


    /**
     * Gets the TickLogger.
     * @return a copy of the TickLogger
     */
    public TickLogger getTickLogger(){
        return tickLogger.copy();
    }

    /**
     * Enables or disables the profiler.
     * This method will block if it's not ran on the main thread.
     *
     * BE WARNED: If you're in another thread, AND the server thread is WAITING (blocked) on your current thread,
     * this will cause a deadlock!
     *
     * Example: net.minecraftforge.common.chunkio.ChunkIOProvider -- Chunk I/O Executor Thread
     *
     *
     * @param shouldProfile if the profiler should be enabled
     */
    public synchronized void setProfileEnabled(boolean shouldProfile){
        Tiquality.SCHEDULER.scheduleWait(new Runnable() {
            @Override
            public void run() {
                if(PlayerTracker.this.isProfiling != shouldProfile) {
                    PlayerTracker.this.isProfiling = shouldProfile;
                    if(shouldProfile == false){
                        MinecraftForge.EVENT_BUS.post(new TiqualityEvent.ProfileCompletedEvent(PlayerTracker.this, getTickLogger()));
                    }else{
                        tickLogger.reset();
                    }
                }
            }
        });
    }

    /**
     * Stops the profiler, and gets it's TickLogger.
     * This method will block if it's not ran on the main thread.
     *
     * BE WARNED: If you're in another thread, AND the server thread is WAITING (blocked) on your current thread,
     * this will cause a deadlock!
     *
     * Example: net.minecraftforge.common.chunkio.ChunkIOProvider -- Chunk I/O Executor Thread
     *
     * @return The TickLogger, or null if the profiler was never running to begin with.
     *
     */
    public synchronized @Nullable TickLogger stopProfiler(){
        return SynchronizedAction.run(new SynchronizedAction.Action<TickLogger>() {
            @Override
            public void run(SynchronizedAction.DynamicVar<TickLogger> variable) {
                if(PlayerTracker.this.isProfiling == true) {
                    PlayerTracker.this.isProfiling = false;
                    MinecraftForge.EVENT_BUS.post(new TiqualityEvent.ProfileCompletedEvent(PlayerTracker.this, getTickLogger()));
                    variable.set(getTickLogger());
                }
            }
        });
    }

    /**
     * Resets every tick with a granted number of tick time set by Tiquality
     * Is initialized with time for a full tick. (Loading blocks mid-tick, or something like that)
     * @param granted_ns the amount of time set for the coming tick in nanoseconds
     */
    public void setNextTickTime(long granted_ns){
        tick_time_remaining_ns = granted_ns;
        if(isProfiling) {
            tickLogger.addTick(granted_ns);
        }
        if(unloadCooldown > 0){
            --unloadCooldown;
        }
    }

    /**
     * Checks if the owner is a fake owner.
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
            if(isProfiling) {
                TiqualitySimpleTickable tickable = untickedTickables.take();
                long start = System.nanoTime();
                tickable.doUpdateTick();
                long elapsed = System.nanoTime() - start;
                tickLogger.addNanosAndIncrementCalls(tickable.getLocation(), elapsed);
                consume(elapsed);
            }else{
                long start = System.nanoTime();
                untickedTickables.take().doUpdateTick();
                consume(System.nanoTime() - start);
            }
        }
        return tick_time_remaining_ns >= 0;
    }

    /**
     * Decides whether or not to tick, based on
     * the time the player has already consumed.
     * @param tickable the TiqualitySimpleTickable object (Tile Entities are castable.)
     */
    public void tickTileEntity(TiqualitySimpleTickable tickable){
        if (updateOld() == false && TiqualityConfig.QuickConfig.TICKFORCING_OBJECTS_FAST.contains(tickable.getLocation().getBlock()) == false){
            /* This PlayerTracker ran out of time, we queue the blockupdate for another tick.*/
            if (untickedTickables.containsRef(tickable) == false) {
                untickedTickables.addToQueue(tickable);
            }
        }else{
            /* Either We still have time, or the tile entity is on the forced-tick list. We update the tile entity.*/
            if(isProfiling) {
                long start = System.nanoTime();
                tickable.doUpdateTick();
                long elapsed = System.nanoTime() - start;
                tickLogger.addNanosAndIncrementCalls(tickable.getLocation(), elapsed);
                consume(elapsed);
            }else{
                long start = System.nanoTime();
                tickable.doUpdateTick();
                consume(System.nanoTime() - start);
            }
        }
    }

    /**
     * Performs block tick if it can, if not, it will queue it for later.
     * @param block the block
     * @param world the world
     * @param pos the block position
     * @param state the block's state
     * @param rand a Random
     */
    public void doBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        if(updateOld() == false && TiqualityConfig.QuickConfig.TICKFORCING_OBJECTS_FAST.contains(block) == false){
            /* This PlayerTracker ran out of time, we queue the blockupdate for another tick.*/
            BlockUpdateHolder holder = new BlockUpdateHolder(block, world, pos, state, rand);
            if (untickedTickables.contains(holder) == false) {
                untickedTickables.addToQueue(holder);

                //ServerSideEvents.showBlocked(world, pos);
            }
        }else{
            /* Either We still have time, or the block is on the forced-tick list. We update the block*/
            if(isProfiling) {
                long start = System.nanoTime();
                block.updateTick(world, pos, state, rand);
                long elapsed = System.nanoTime() - start;
                tickLogger.addNanosAndIncrementCalls(new TickLogger.Location(world, pos), elapsed);
                consume(elapsed);
            }else{
                long start = System.nanoTime();
                block.updateTick(world, pos, state, rand);
                consume(System.nanoTime() - start);
            }
        }
    }

    /**
     * Performs block tick if it can, if not, it will queue it for later.
     * @param block the block
     * @param world the world
     * @param pos the block position
     * @param state the block's state
     * @param rand a Random
     */
    public void doRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        if(updateOld() == false && TiqualityConfig.QuickConfig.TICKFORCING_OBJECTS_FAST.contains(block) == false){
            /* This PlayerTracker ran out of time, we queue the blockupdate for another tick.*/
            BlockRandomUpdateHolder holder = new BlockRandomUpdateHolder(block, world, pos, state, rand);
            if (untickedTickables.contains(holder) == false) {
                untickedTickables.addToQueue(holder);



                //ServerSideEvents.showBlocked(world, pos);
            }
        }else{
            /* Either We still have time, or the block is on the forced-tick list. We update the block*/
            if(isProfiling) {
                long start = System.nanoTime();
                block.randomTick(world, pos, state, rand);
                long elapsed = System.nanoTime() - start;
                tickLogger.addNanosAndIncrementCalls(new TickLogger.Location(world, pos), elapsed);
                consume(elapsed);
            }else{
                long start = System.nanoTime();
                block.randomTick(world, pos, state, rand);
                consume(System.nanoTime() - start);
            }
        }
    }

    /**
     * After running out of tick time for this player, the server may have more
     * tick time to spare after ticking other players, it grants unchecked ticks
     */
    public void grantTick(){
        if(untickedTickables.size() > 0) {
            if(isProfiling) {
                TiqualitySimpleTickable tickable = untickedTickables.take();
                long start = System.nanoTime();
                tickable.doUpdateTick();
                long elapsed = System.nanoTime() - start;
                tickLogger.addNanosAndIncrementCalls(tickable.getLocation(), elapsed);
            }else{
                untickedTickables.take().doUpdateTick();
            }
        }
    }

    /**
     * Associates chunks with this PlayerTracker.
     * The player tracker will only be garbage collected when all associated chunks are unloaded.
     * @param chunk the chunk.
     */
    public void associateChunk(TiqualityChunk chunk){
        unloadCooldown = 40;
        ASSOCIATED_CHUNKS.add(chunk);
    }

    /**
     * Removes associated chunks with this PlayerTracker.
     * The player tracker will only be garbage collected when all associated chunks are unloaded.
     * @param chunk the chunk.
     */
    public void disAssociateChunk(TiqualityChunk chunk){
        ASSOCIATED_CHUNKS.remove(chunk);
    }

    /**
     * Checks if this PlayerTracker has chunks associated with it,
     * removes references to unloaded chunks,
     * @return true if this PlayerTracker has a loaded chunk, false otherwise
     */
    public boolean isLoaded(){
        if(unloadCooldown > 0){
            return true;
        }
        HashSet<TiqualityChunk> loadedChunks = new HashSet<>();
        for(TiqualityChunk chunk : ASSOCIATED_CHUNKS){
            if(chunk.isChunkLoaded() == true){
                loadedChunks.add(chunk);
            }
        }
        ASSOCIATED_CHUNKS.retainAll(loadedChunks);
        return ASSOCIATED_CHUNKS.size() > 0;
    }

    /**
     * @return true if the PlayerTracker has completed all of it's work.
     */
    public boolean isDone(){
        return untickedTickables.size() == 0;
    }

    /**
     * Debugging method. Do not use in production environments.
     * @return description
     */
    public String toString(){
        return "PlayerTracker:{Owner: '" + getOwner().getName() + "', nsleft: " + tick_time_remaining_ns + ", unticked: " + untickedTickables.size() + ", hashCode: " + System.identityHashCode(this) + "}";
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
