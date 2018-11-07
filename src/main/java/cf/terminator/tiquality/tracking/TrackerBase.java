package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.api.TiqualityException;
import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.util.Constants;
import cf.terminator.tiquality.util.FiFoQueue;
import cf.terminator.tiquality.util.PersistentData;
import cf.terminator.tiquality.util.SynchronizedAction;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public abstract class TrackerBase {

    /**
     * There's a theoretical maximum of 1.8446744e+19 different Trackers per server. This should suffice.
     */
    private static long NEXT_TRACKER_ID;
    static {
        if(PersistentData.NEXT_FREE_TRACKER_ID.isSet() == false){
            PersistentData.NEXT_FREE_TRACKER_ID.setLong(Long.MIN_VALUE);
        }
        NEXT_TRACKER_ID = PersistentData.NEXT_FREE_TRACKER_ID.getLong();
    }

    /**
     * Holds a list of all registered trackers
     * See: cf.terminator.tiquality.api.Tracking#registerCustomTracker(java.lang.Class)
     */
    public static final HashMap<String, Class<? extends TrackerBase>> REGISTERED_TRACKER_TYPES = new HashMap<>();

    private long uniqueId;
    protected long tick_time_remaining_ns = Constants.NS_IN_TICK_LONG;
    protected FiFoQueue<TiqualitySimpleTickable> untickedTickables = new FiFoQueue<>();
    protected final HashSet<TiqualityChunk> ASSOCIATED_CHUNKS = new HashSet<>();
    protected TickLogger tickLogger = new TickLogger();

    public long getUniqueId(){
        return uniqueId;
    }

    public static long generateUniqueTrackerID(){
        synchronized (PersistentData.NEXT_FREE_TRACKER_ID) {
            long granted = NEXT_TRACKER_ID++;
            PersistentData.NEXT_FREE_TRACKER_ID.setLong(NEXT_TRACKER_ID);
            return granted;
        }
    }

    public static NBTTagCompound getTrackerTag(TrackerBase tracker){
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", tracker.getIdentifier());
        tag.setLong("id", tracker.getUniqueId());
        tag.setTag("data", tracker.getNBT());
        return tag;
    }

    void setUniqueId(long id){
        uniqueId = id;
    }

    /**
     * Tiquality only saves trackers to disk if they return true here.
     * @return true if your tracker should be saved to disk.
     */
    public boolean shouldSaveToDisk(){
        return true;
    }

    /**
     * A default constructor for Tracker elements.
     */
    public TrackerBase(){
        uniqueId = generateUniqueTrackerID();
    }

    /**
     * Used to initialize a new Tracker with saved data, if this constructor isn't overridden, I complain.
     * @param world The world
     * @param tag the NBTTagCompound. (generated using the getNBT method on the last save)
     */
    public TrackerBase(TiqualityWorld world, NBTTagCompound tag){
        super();
        throw new TiqualityException.ReadTheDocsException("You MUST define a constructor using an NBTTagCompound as argument: " + getClass());
    }

    /**
     * Gets the NBT data from this object, is called when the tracker is saved to disk.
     */
    public abstract NBTTagCompound getNBT();

    /**
     * Internal use only. Used to determine when to unload.
     */
    private int unloadCooldown = 20;

    /**
     * Only changes between ticks
     */
    protected boolean isProfiling = false;

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
     * Gets the tick time multiplier for the Tracker.
     * This is used to distribute tick time in a more controlled manner.
     * @param cache The current online player cache
     * @return the multiplier
     */
    public abstract double getMultiplier(final GameProfile[] cache);

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
     * the time the tracker has already consumed.
     * @param tickable the TiqualitySimpleTickable object (Tile Entities are castable.)
     */
    public void tickTileEntity(TiqualitySimpleTickable tickable){
        if (updateOld() == false && TiqualityConfig.QuickConfig.TICKFORCING_OBJECTS_FAST.contains(tickable.getLocation().getBlock()) == false){
            /* This Tracker ran out of time, we queue the blockupdate for another tick.*/
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
    public void tickEntity(TiqualityEntity entity){
        if (updateOld() == false){
            /* This Tracker ran out of time, we queue the entity update for another tick.*/
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
    public void doBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        if(updateOld() == false && TiqualityConfig.QuickConfig.TICKFORCING_OBJECTS_FAST.contains(block) == false){
            /* This Tracker ran out of time, we queue the blockupdate for another tick.*/
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
            /* This Tracker ran out of time, we queue the blockupdate for another tick.*/
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
     * After running out of tick time for this Tracker, the server may have more
     * tick time to spare after ticking other Trackers, it grants unchecked ticks
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
     * Associates chunks with this Tracker.
     * The tracker will only be garbage collected when all associated chunks are unloaded.
     * @param chunk the chunk.
     */
    public void associateChunk(TiqualityChunk chunk){
        unloadCooldown = 40;
        synchronized (ASSOCIATED_CHUNKS) {
            ASSOCIATED_CHUNKS.add(chunk);
        }
    }

    /**
     * Removes associated chunks with this Tracker.
     * The tracker will only be garbage collected when all associated chunks are unloaded.
     * @param chunk the chunk.
     */
    public void disAssociateChunk(TiqualityChunk chunk){
        synchronized (ASSOCIATED_CHUNKS) {
            ASSOCIATED_CHUNKS.remove(chunk);
        }
    }

    /**
     * Checks if this Tracker has chunks associated with it,
     * removes references to unloaded chunks,
     * @return true if this Tracker has a loaded chunk, false otherwise
     */
    public boolean isLoaded(){
        if(unloadCooldown > 0){
            return true;
        }
        HashSet<TiqualityChunk> loadedChunks = new HashSet<>();
        synchronized (ASSOCIATED_CHUNKS) {
            for (TiqualityChunk chunk : ASSOCIATED_CHUNKS) {
                if (chunk.isChunkLoaded() == true) {
                    loadedChunks.add(chunk);
                }
            }
            ASSOCIATED_CHUNKS.retainAll(loadedChunks);
            return ASSOCIATED_CHUNKS.size() > 0;
        }
    }

    /**
     * Gets the associated players for this tracker
     * @return a list of all players involved with this tracker.
     */
    @Nonnull
    public abstract List<GameProfile> getAssociatedPlayers();

    /**
     * Used to determine if it's safe to unload this tracker
     * @return true if the Tracker has completed all of it's work.
     */
    public boolean isDone(){
        return untickedTickables.size() == 0;
    }

    /**
     * Debugging method. Do not use in production environments.
     * @return description
     */
    public String toString(){
        return this.getClass() + ":{nsleft: " + tick_time_remaining_ns + ", unticked: " + untickedTickables.size() + ", hashCode: " + System.identityHashCode(this) + "}";
    }

    /**
     * @return the info describing this Tracker (Like the owner)
     */
    @Nonnull
    public abstract TextComponentString getInfo();

    /**
     * @return an unique identifier for this Tracker CLASS TYPE, used to re-instantiate the tracker later on.
     * This should just return a hardcoded string.
     */
    @Nonnull
    public String getIdentifier(){
        throw new TiqualityException.ReadTheDocsException("You are required to implement 'public static String getIdentifier()' using a string constant in your Tracker.");
    }

    /**
     * Checks if this tracker should be unloaded, overrides all other checks
     * @return false to keep this tracker from being garbage collected, true otherwise.
     */
    public boolean forceUnload() {
        return false;
    }

    /**
     * Ran when this tracker is being unloaded. Do cleanup here, if you have to.
     */
    public void onUnload() {
    }
}
