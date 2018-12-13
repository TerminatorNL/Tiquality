package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.memory.WeakReferencedChunk;
import cf.terminator.tiquality.memory.WeakReferencedTracker;
import cf.terminator.tiquality.tracking.update.BlockRandomUpdateHolder;
import cf.terminator.tiquality.tracking.update.BlockUpdateHolder;
import cf.terminator.tiquality.util.Constants;
import cf.terminator.tiquality.util.FiFoQueue;
import cf.terminator.tiquality.util.SynchronizedAction;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.HashSet;
import java.util.Random;

public abstract class TrackerBase implements Tracker {

    /**
     * When the TrackerManager forgets the tracker permanently, this will become true.
     * Do not set this manually.
     */
    protected boolean isUnloaded = false;

    protected long tick_time_remaining_ns = Constants.NS_IN_TICK_LONG;
    protected FiFoQueue<TiqualitySimpleTickable> untickedTickables = new FiFoQueue<>();
    protected final HashSet<WeakReferencedChunk> ASSOCIATED_CHUNKS = new HashSet<>();
    protected final HashSet<WeakReferencedTracker> DELEGATING_TRACKERS = new HashSet<>();
    protected TickLogger tickLogger = new TickLogger();
    private TrackerHolder holder;

    public void setHolder(TrackerHolder holder) {
        if(this.holder != null){
            throw new IllegalStateException("Attempt to bind two holders to one tracker.");
        }
        this.holder = holder;
    }

    public TrackerHolder getHolder(){
        return holder;
    }

    /**
     * Tiquality only saves trackers to disk if they return true here.
     * @return true if your tracker should be saved to disk.
     */
    @Override
    public boolean shouldSaveToDisk(){
        return true;
    }

    public TrackerBase(){
    }

    /**
     * Internal use only. Used to determine when to unload.
     */
    private int unloadCooldown = 40;

    /**
     * Only changes between ticks
     */
    protected boolean isProfiling = false;

    /**
     * Gets the TickLogger.
     * @return a copy of the TickLogger
     */
    @Override
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
    @Override
    public synchronized void setProfileEnabled(boolean shouldProfile){
        Tiquality.SCHEDULER.scheduleWait(new Runnable() {
            @Override
            public void run() {
                if(TrackerBase.this.isProfiling != shouldProfile) {
                    TrackerBase.this.isProfiling = shouldProfile;
                    if(shouldProfile == false){
                        MinecraftForge.EVENT_BUS.post(new TiqualityEvent.ProfileCompletedEvent(TrackerBase.this, getTickLogger()));
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
    @Override
    public synchronized @Nullable TickLogger stopProfiler(){
        return SynchronizedAction.run(new SynchronizedAction.Action<TickLogger>() {
            @Override
            public void run(SynchronizedAction.DynamicVar<TickLogger> variable) {
                if(TrackerBase.this.isProfiling == true) {
                    TrackerBase.this.isProfiling = false;
                    MinecraftForge.EVENT_BUS.post(new TiqualityEvent.ProfileCompletedEvent(TrackerBase.this, getTickLogger()));
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
    @Override
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
     * Decreases the remaining tick time for a tracker.
     * @param time in nanoseconds
     */
    public void consume(long time){
        tick_time_remaining_ns -= time;
    }

    /**
     * Gets the remaining tick time this tracker has.
     * Can be compared against the set tick time to
     * check if there are any active ticking entities.
     *
     * @return the remaining tick time, in nanoseconds.
     */
    @Override
    public long getRemainingTime(){
        return tick_time_remaining_ns;
    }

    /**
     * Updates the queued items first.
     * @return true if everything was updated, and there is more time left.
     */
    public boolean updateOld(){
        while(untickedTickables.size() > 0 && tick_time_remaining_ns > 0) {
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
        return tick_time_remaining_ns > 0;
    }

    /**
     * Decides whether or not to tick, based on
     * the time the tracker has already consumed.
     * @param tickable the TiqualitySimpleTickable object (Tile Entities are castable.)
     */
    @Override
    public void tickTileEntity(TiqualitySimpleTickable tickable){
        if (updateOld() == false && TiqualityConfig.QuickConfig.TICKFORCING_OBJECTS_FAST.contains(tickable.getLocation().getBlock()) == false){
            /* This TrackerBase ran out of time, we queue the blockupdate for another tick.*/
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
     * Decides whether or not to tick, based on
     * the time the tracker has already consumed.
     * @param entity the Entity to tick
     */
    @Override
    public void tickEntity(TiqualityEntity entity){
        if(isUnloaded){
            entity.doUpdateTick();
            entity.setTracker(null);
            return;
        }
        if (updateOld() == false){
            /* This TrackerBase ran out of time, we queue the entity update for another tick.*/
            if (untickedTickables.containsRef(entity) == false) {
                untickedTickables.addToQueue(entity);
            }
        }else{
            /* Either We still have time, or the tile entity is on the forced-tick list. We update the entity.*/
            if(isProfiling) {
                long start = System.nanoTime();
                entity.doUpdateTick();
                long elapsed = System.nanoTime() - start;
                tickLogger.addNanosAndIncrementCalls(entity.getLocation(), elapsed);
                consume(elapsed);
            }else{
                long start = System.nanoTime();
                entity.doUpdateTick();
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
    @Override
    public void doBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        if(updateOld() == false && TiqualityConfig.QuickConfig.TICKFORCING_OBJECTS_FAST.contains(block) == false){
            /* This TrackerBase ran out of time, we queue the blockupdate for another tick.*/
            BlockUpdateHolder holder = new BlockUpdateHolder(block, world, pos, state, rand);
            if (untickedTickables.contains(holder) == false) {
                untickedTickables.addToQueue(holder);

                //ServerSideEvents.showBlocked(world, pos);
            }
        }else{
            /* Either We still have time, or the block is on the forced-tick list. We update the block*/
            if(isProfiling) {
                long start = System.nanoTime();
                Tiquality.TICK_EXECUTOR.onBlockTick(block, world, pos, state, rand);
                long elapsed = System.nanoTime() - start;
                tickLogger.addNanosAndIncrementCalls(new TickLogger.Location(world, pos), elapsed);
                consume(elapsed);
            }else{
                long start = System.nanoTime();
                Tiquality.TICK_EXECUTOR.onBlockTick(block, world, pos, state, rand);
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
    @Override
    public void doRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        if(updateOld() == false && TiqualityConfig.QuickConfig.TICKFORCING_OBJECTS_FAST.contains(block) == false){
            /* This TrackerBase ran out of time, we queue the blockupdate for another tick.*/
            BlockRandomUpdateHolder holder = new BlockRandomUpdateHolder(block, world, pos, state, rand);
            if (untickedTickables.contains(holder) == false) {
                untickedTickables.addToQueue(holder);



                //ServerSideEvents.showBlocked(world, pos);
            }
        }else{
            /* Either We still have time, or the block is on the forced-tick list. We update the block*/
            if(isProfiling) {
                long start = System.nanoTime();
                Tiquality.TICK_EXECUTOR.onRandomBlockTick(block, world, pos, state, rand);
                long elapsed = System.nanoTime() - start;
                tickLogger.addNanosAndIncrementCalls(new TickLogger.Location(world, pos), elapsed);
                consume(elapsed);
            }else{
                long start = System.nanoTime();
                Tiquality.TICK_EXECUTOR.onRandomBlockTick(block, world, pos, state, rand);
                consume(System.nanoTime() - start);
            }
        }
    }

    /**
     * After running out of tick time for this TrackerBase, the server may have more
     * tick time to spare after ticking other Trackers, it grants unchecked ticks
     */
    @Override
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
     * Associates chunks with this TrackerBase.
     * The tracker will only be garbage collected when all associated chunks are unloaded.
     * @param chunk the chunk.
     */
    @Override
    public void associateChunk(TiqualityChunk chunk){
        unloadCooldown = 40;
        synchronized (ASSOCIATED_CHUNKS) {
            ASSOCIATED_CHUNKS.add(new WeakReferencedChunk(chunk));
        }
    }

    /**
     * Associates another Tracker with this TrackerBase.
     * The tracker will only be garbage collected when all delegating trackers are unloaded.
     * Delegators are trackers that use other trackers (this one) for their data management.
     * @param tracker the chunk.
     */
    @Override
    public void associateDelegatingTracker(Tracker tracker){
        unloadCooldown = 40;
        synchronized (DELEGATING_TRACKERS) {
            DELEGATING_TRACKERS.add(new WeakReferencedTracker(tracker));
        }
    }

    /**
     * Removes an associated tracker (When changing ownership, for example.)
     * @param tracker the chunk.
     */
    @Override
    public void removeDelegatingTracker(Tracker tracker){
        synchronized (DELEGATING_TRACKERS) {
            DELEGATING_TRACKERS.removeIf(t -> tracker.equals(t.get()));
        }
    }

    /**
     * Checks if this TrackerBase has chunks associated with it and is kept in memory by the TrackerManager.
     * Also removes references to unloaded chunks and unloaded delegating trackers.
     * @return true if this TrackerBase has a loaded chunk or the cooldown is not over yet, false otherwise
     */
    public boolean isLoaded(){
        if(isUnloaded){
            return false;
        }
        synchronized (ASSOCIATED_CHUNKS) {
            ASSOCIATED_CHUNKS.removeIf(chunk -> chunk.isChunkLoaded() == false);
            if(ASSOCIATED_CHUNKS.size() > 0){
                return true;
            }
        }
        synchronized (DELEGATING_TRACKERS) {
            DELEGATING_TRACKERS.removeIf(tracker -> tracker.exists() == false);
            if(DELEGATING_TRACKERS.size() > 0){
                return true;
            }
        }
        return false;
    }

    /**
     * Debugging method. Do not use in production environments.
     * @return description
     */
    @Override
    public String toString(){
        return this.getClass() + ":{nsleft: " + tick_time_remaining_ns + ", unticked: " + untickedTickables.size() + ", hashCode: " + System.identityHashCode(this) + "}";
    }

    /**
     * Checks if this tracker should be unloaded, overrides all other checks
     * @return false to keep this tracker from being garbage collected, true otherwise.
     */
    @Override
    public boolean shouldUnload() {
        return isLoaded() == false && unloadCooldown == 0;
    }

    @Override
    public boolean needsTick(){
        return untickedTickables.size() > 0;
    }

    /**
     * Ran when this tracker is being unloaded. Do cleanup here, if you have to.
     */
    @Override
    @OverridingMethodsMustInvokeSuper
    public void onUnload() {
        isUnloaded = true;

        /*
            We tick all remaining tickables to minimize chances on undefined behavior from mods
         */
        while(untickedTickables.size() > 0){
            untickedTickables.take().doUpdateTick();
        }
    }

    @Override
    public int compareTo(@Nonnull Object o) {
        if(o instanceof TrackerBase == false){
            return -1;
        }
        return Long.compare(this.getHolder().getId(), ((TrackerBase) o).getHolder().getId());
    }
}
