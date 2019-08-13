package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.TiqualityException;
import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.interfaces.*;
import cf.terminator.tiquality.memory.WeakReferencedChunk;
import cf.terminator.tiquality.memory.WeakReferencedTracker;
import cf.terminator.tiquality.profiling.ProfilingKey;
import cf.terminator.tiquality.profiling.TickLogger;
import cf.terminator.tiquality.tracking.tickqueue.TickQueue;
import cf.terminator.tiquality.tracking.update.BlockRandomUpdateHolder;
import cf.terminator.tiquality.tracking.update.BlockUpdateHolder;
import cf.terminator.tiquality.util.Constants;
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
    public TickQueue tickQueue = new TickQueue(this);
    protected final HashSet<WeakReferencedChunk> ASSOCIATED_CHUNKS = new HashSet<>();
    protected final HashSet<WeakReferencedTracker> DELEGATING_TRACKERS = new HashSet<>();
    protected TickLogger tickLogger = new TickLogger();
    private TrackerHolder holder;
    @Nullable
    private ProfilingKey key;

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

    @Override
    public void tick() {
        tickQueue.notifyNextTick();
    }

    @Override
    public boolean canProfile(){
        return true;
    }

    /**
     * Internal use only. Used to determine when to unload.
     */
    private int unloadCooldown = 40;

    /**
     * Only changes between ticks
     */
    private boolean isProfiling = false;

    @Override
    public boolean isProfiling() {
        return isProfiling;
    }

    @Override
    public long getProfileEndTime() {
        return key == null ? 0 : key.getProfileEndTime();
    }

    @Nonnull
    @Override
    public ProfilingKey startProfiler(long profileEndTime) throws TiqualityException.TrackerAlreadyProfilingException {
        if(isProfiling){
            throw new TiqualityException.TrackerAlreadyProfilingException(this);
        }
        this.key = new ProfilingKey(profileEndTime);
        Tiquality.SCHEDULER.schedule(new Runnable() {
            @Override
            public void run() {
                isProfiling = true;
            }
        });
        return key;
    }

    @Nonnull
    @Override
    public TickLogger stopProfiler(ProfilingKey key) throws TiqualityException.InvalidKeyException {
        if(this.key != key){
            throw new TiqualityException.InvalidKeyException(this, this.key);
        }
        return SynchronizedAction.run(new SynchronizedAction.Action<TickLogger>() {
            @Override
            public void run(SynchronizedAction.DynamicVar<TickLogger> variable) {
                if(isProfiling == true) {
                    isProfiling = false;
                    TrackerBase.this.key = null;
                    MinecraftForge.EVENT_BUS.post(new TiqualityEvent.ProfileCompletedEvent(TrackerBase.this, tickLogger));
                    variable.set(tickLogger);
                    tickLogger = new TickLogger();
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
    @OverridingMethodsMustInvokeSuper
    public void setNextTickTime(long granted_ns){
        tick_time_remaining_ns = granted_ns;
        if(isProfiling) {
            tickLogger.addServerTick(granted_ns);
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
        while(tickQueue.size() > 0 && getRemainingTime() > 0) {
            TiqualitySimpleTickable tickable = tickQueue.take();
            if(tickable.tiquality_isLoaded() == false){
                continue;
            }
            if(isProfiling) {
                long start = System.nanoTime();
                tickable.tiquality_doUpdateTick();
                long elapsed = System.nanoTime() - start;
                tickLogger.addNanosAndIncrementCalls(tickable.getId(), elapsed);
                consume(elapsed);
            }else{
                long start = System.nanoTime();
                tickable.tiquality_doUpdateTick();
                consume(System.nanoTime() - start);
            }
        }
        return getRemainingTime() > 0;
    }

    @Override
    public void addTickableToQueue(TiqualitySimpleTickable tickable){
        if(tickQueue.containsSimpleUpdate(tickable)){
            tickQueue.addToQueue(tickable);
        }
    }

    /**
     * Decides whether or not to tick, based on
     * the time the tracker has already consumed.
     * @param tileEntity the TiqualityExtendedTickable object (Tile Entities are castable.)
     */
    @Override
    public void tickSimpleTickable(TiqualitySimpleTickable tileEntity){
        if(updateOld() == false && tileEntity.getUpdateType().mustTick(this) == false){
            /* This Tracker ran out of time, we queue the blockupdate for another tick.*/
            if (tickQueue.containsTileEntityUpdate(tileEntity) == false) {
                tickQueue.addToQueue(tileEntity);
            }
        }else{
            /* Either We still have time, or the tile entity is on the forced-tick list. We update the tile entity.*/
            if(isProfiling) {
                long start = System.nanoTime();
                tileEntity.tiquality_doUpdateTick();
                long elapsed = System.nanoTime() - start;
                tickLogger.addNanosAndIncrementCalls(tileEntity.getId(), elapsed);
                consume(elapsed);
            }else{
                long start = System.nanoTime();
                tileEntity.tiquality_doUpdateTick();
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
            entity.tiquality_doUpdateTick();
            entity.setTrackerHolder(null);
            return;
        }
        if (updateOld() == false && entity.getUpdateType().mustTick(this) == false){
            /* This Tracker ran out of time, we queue the entity update for another tick.*/
            if (tickQueue.containsEntityUpdate(entity) == false) {
                tickQueue.addToQueue(entity);
            }
        }else{
            /* Either We still have time, or the tile entity is on the forced-tick list. We update the entity.*/
            if(isProfiling) {
                long start = System.nanoTime();
                entity.tiquality_doUpdateTick();
                long elapsed = System.nanoTime() - start;
                tickLogger.addNanosAndIncrementCalls(entity.getId(), elapsed);
                consume(elapsed);
            }else{
                long start = System.nanoTime();
                entity.tiquality_doUpdateTick();
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
        UpdateType updateType = ((UpdateTyped) block).getUpdateType();
        if(updateOld() == false && updateType.mustTick(this) == false){
            /* This Tracker ran out of time, we queue the blockupdate for another tick.*/
            if (tickQueue.containsBlockUpdate(((TiqualityWorld) world), pos) == false) {
                tickQueue.addToQueue(new BlockUpdateHolder(world, pos, rand, updateType));
                //ServerSideEvents.showBlocked(world, pos);
            }
        }else{
            /* Either We still have time, or the block is on the forced-tick list. We update the block*/
            if(isProfiling) {
                long start = System.nanoTime();
                Tiquality.TICK_EXECUTOR.onBlockTick(block, world, pos, state, rand);
                long elapsed = System.nanoTime() - start;
                tickLogger.addNanosAndIncrementCalls(BlockUpdateHolder.getId(world.provider.getDimension(), pos), elapsed);
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
        UpdateType updateType = ((UpdateTyped) block).getUpdateType();
        if(updateOld() == false && updateType.mustTick(this) == false){
            /* This Tracker ran out of time, we queue the blockupdate for another tick.*/
            if (tickQueue.containsRandomBlockUpdate(((TiqualityWorld) world), pos) == false) {
                tickQueue.addToQueue(new BlockRandomUpdateHolder(world, pos, rand, updateType));



                //ServerSideEvents.showBlocked(world, pos);
            }
        }else{
            /* Either We still have time, or the block is on the forced-tick list. We update the block*/
            if(isProfiling) {
                long start = System.nanoTime();
                Tiquality.TICK_EXECUTOR.onRandomBlockTick(block, world, pos, state, rand);
                long elapsed = System.nanoTime() - start;
                tickLogger.addNanosAndIncrementCalls( BlockRandomUpdateHolder.getId(world.provider.getDimension(), pos), elapsed);
                consume(elapsed);
            }else{
                long start = System.nanoTime();
                Tiquality.TICK_EXECUTOR.onRandomBlockTick(block, world, pos, state, rand);
                consume(System.nanoTime() - start);
            }
        }
    }

    /**
     * After running out of tick time for this Tracker, the server may have more
     * tick time to spare after ticking other Trackers, it grants unchecked ticks
     */
    @Override
    public void grantTick(){
        if(tickQueue.size() > 0) {
            TiqualitySimpleTickable tickable = tickQueue.take();
            if(tickable.tiquality_isLoaded() == false){
                return;
            }
            if(isProfiling) {
                long start = System.nanoTime();
                tickable.tiquality_doUpdateTick();
                long elapsed = System.nanoTime() - start;
                tickLogger.addNanosAndIncrementCalls(tickable.getId(), elapsed);
            }else{
                tickable.tiquality_doUpdateTick();
            }
        }
    }

    /**
     * Associates chunks with this Tracker.
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
     * Associates another Tracker with this Tracker.
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
     * Checks if this Tracker has chunks associated with it and is kept in memory by the TrackerManager.
     * Also removes references to unloaded chunks and unloaded delegating trackers.
     * @return true if this Tracker has a loaded chunk or the cooldown is not over yet, false otherwise
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
        return this.getClass() + ":{nsleft: " + getRemainingTime() + ", unticked: " + tickQueue.size() + ", hashCode: " + System.identityHashCode(this) + "}";
    }

    /**
     * Checks if this tracker should be unloaded, overrides all other checks
     * @return false to keep this tracker from being garbage collected, true otherwise.
     */
    @Override
    public boolean shouldUnload() {
        return isLoaded() == false && unloadCooldown == 0 && isProfiling == false;
    }

    @Override
    public boolean needsTick(){
        return tickQueue.size() > 0;
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
        tickQueue.tickAll();
    }

    @Nonnull
    public TickLogger getTickLogger() throws TiqualityException.TrackerWasNotProfilingException {
        if(isProfiling == false){
            throw new TiqualityException.TrackerWasNotProfilingException(this);
        }else{
            return tickLogger;
        }
    }

}
