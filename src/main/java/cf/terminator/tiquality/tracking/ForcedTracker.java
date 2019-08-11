package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.TiqualityException;
import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.interfaces.*;
import cf.terminator.tiquality.profiling.ProfilingKey;
import cf.terminator.tiquality.profiling.TickLogger;
import cf.terminator.tiquality.tracking.update.BlockRandomUpdateHolder;
import cf.terminator.tiquality.tracking.update.BlockUpdateHolder;
import cf.terminator.tiquality.util.SynchronizedAction;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A class used to Track unowned updates for profiling purposes.
 * This TrackerBase does not throttle it's tickables.
 */
public class ForcedTracker implements Tracker {

    public static final ForcedTracker INSTANCE = new ForcedTracker();
    private boolean isProfiling = false;
    private TickLogger tickLogger = new TickLogger();
    private ProfilingKey key;

    protected ForcedTracker() {
    }

    /**
     * Gets the NBT data from this object, is called when the tracker is saved to disk.
     */
    @Nonnull
    @Override
    public NBTTagCompound getNBT() {
        return new NBTTagCompound();
    }

    @Nonnull
    @Override
    public ProfilingKey startProfiler(long profileEndTime) throws TiqualityException.TrackerAlreadyProfilingException {
        if(isProfiling){
            throw new TiqualityException.TrackerAlreadyProfilingException(this);
        }
        key = new ProfilingKey(profileEndTime);
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
                    MinecraftForge.EVENT_BUS.post(new TiqualityEvent.ProfileCompletedEvent(ForcedTracker.this, tickLogger));
                    variable.set(tickLogger);
                    tickLogger = new TickLogger();
                }
            }
        });
    }

    @Override
    public boolean canProfile() {
        return true;
    }

    @Override
    public boolean isProfiling() {
        return isProfiling;
    }

    @Override
    public long getProfileEndTime() {
        return key == null ? 0 : key.getProfileEndTime();
    }


    @Override
    public void setNextTickTime(long granted_ns) {
        if(isProfiling) {
            tickLogger.addServerTick(granted_ns);
        }
    }

    @Override
    public Tracker load(TiqualityWorld world, NBTTagCompound trackerTag) {
        throw new UnsupportedOperationException("How can ForcedTracker be loaded, when it is never saved?");
    }

    /**
     * We don't want this to be saved to disk, due to config options.
     * Because we don't save to disk, we don't need the constructor(TiqualityChunk, NBTTagCompound)
     * @return false
     */
    @Override
    public boolean shouldSaveToDisk(){
        return false;
    }

    /**
     * Ticks the tile entity, and optionally profiles it.
     * @param tileEntity the TiqualityExtendedTickable object (Tile Entities are castable.)
     */
    @Override
    public void tickSimpleTickable(TiqualitySimpleTickable tileEntity){
        if(isProfiling) {
            long start = System.nanoTime();
            Tiquality.TICK_EXECUTOR.onTileEntityTick((ITickable) tileEntity);
            long elapsed = System.nanoTime() - start;
            tickLogger.addNanosAndIncrementCalls(tileEntity.getId(), elapsed);
        }else{
            Tiquality.TICK_EXECUTOR.onTileEntityTick((ITickable) tileEntity);
        }
    }

    /**
     * Performs block tick, and optionally profiles it
     * @param block the block
     * @param world the world
     * @param pos the block position
     * @param state the block's state
     * @param rand a Random
     */
    @Override
    public void doBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        if(isProfiling) {
            long start = System.nanoTime();
            Tiquality.TICK_EXECUTOR.onBlockTick(block, world, pos, state, rand);
            long elapsed = System.nanoTime() - start;
            tickLogger.addNanosAndIncrementCalls(BlockUpdateHolder.getId(world.provider.getDimension(),pos), elapsed);
        }else{
            Tiquality.TICK_EXECUTOR.onBlockTick(block, world, pos, state, rand);
        }
    }

    /**
     * Performs a random block tick, and optionally profiles it.
     * @param block the block
     * @param world the world
     * @param pos the block position
     * @param state the block's state
     * @param rand a Random
     */
    @Override
    public void doRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        if(isProfiling) {
            long start = System.nanoTime();
            Tiquality.TICK_EXECUTOR.onRandomBlockTick(block, world, pos, state, rand);
            long elapsed = System.nanoTime() - start;
            tickLogger.addNanosAndIncrementCalls(BlockRandomUpdateHolder.getId(world.provider.getDimension(),pos), elapsed);
        }else{
            Tiquality.TICK_EXECUTOR.onRandomBlockTick(block, world, pos, state, rand);
        }
    }

    @Override
    public void grantTick() {
        throw new UnsupportedOperationException("ForcedTracker does not need ticks");
    }

    @Override
    public void addTickableToQueue(TiqualitySimpleTickable tickable) {
        tickable.tiquality_doUpdateTick();
    }

    @Override
    public void associateChunk(TiqualityChunk chunk) {

    }

    @Override
    public void associateDelegatingTracker(Tracker tracker) {
        throw new UnsupportedOperationException("ForcedTracker should not be delegated");
    }

    @Override
    public void removeDelegatingTracker(Tracker tracker) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the associated players for this tracker
     *
     * @return an empty list
     */
    @Nonnull
    @Override
    public List<GameProfile> getAssociatedPlayers() {
        return Collections.emptyList();
    }

    /**
     * Ticks the entity, and optionally profiles it
     * @param entity the Entity to tick
     */
    @Override
    public void tickEntity(TiqualityEntity entity){
        if(isProfiling) {
            long start = System.nanoTime();
            Tiquality.TICK_EXECUTOR.onEntityTick((Entity) entity);
            long elapsed = System.nanoTime() - start;
            tickLogger.addNanosAndIncrementCalls(entity.getId(), elapsed);
        }else{
            Tiquality.TICK_EXECUTOR.onEntityTick((Entity) entity);
        }
    }

    /**
     * Since we're a TrackerBase without an owner, we assign 0 time to it's tick time.
     * @param cache The current online player cache
     * @return 0
     */
    @Override
    public double getMultiplier(final GameProfile[] cache){
        return 0;
    }

    @Override
    public long getRemainingTime() {
        return 0;
    }

    @Override
    public boolean needsTick() {
        return false;
    }

    /**
     * Debugging method. Do not use in production environments.
     * @return description
     */
    @Override
    public String toString(){
        return "ForcedTracker:{hashCode: " + System.identityHashCode(this) + "}";
    }

    /**
     * @return the info describing this TrackerBase (Like the owner)
     */
    @Nonnull
    @Override
    public TextComponentString getInfo() {
        return new TextComponentString(TextFormatting.LIGHT_PURPLE + "Forced");
    }

    /**
     * @return an unique identifier for this TrackerBase CLASS TYPE, used to re-instantiate the tracker later on.
     * This should just return a hardcoded string.
     */
    @Nonnull
    public String getIdentifier() {
        throw new UnsupportedOperationException("Attempt to get the identifier for ForcedTracker.");
    }

    /**
     * Required to check for colission with unloaded trackers.
     *
     * @return int the hash code, just like Object#hashCode().
     */
    @Override
    public int getHashCode() {
        return 0;
    }

    @Override
    public boolean shouldUnload() {
        return false;
    }

    @Override
    public void onUnload() {
        throw new UnsupportedOperationException("Unloading ForcedTracker is never allowed.");
    }

    private TrackerHolder holder = null;

    @Override
    public void setHolder(TrackerHolder holder) {
        this.holder = holder;
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    /**
     * Checks if the tracker is equal to one already in the database.
     * Allows for flexibility for loading.
     *
     * @param tag tag
     * @return equals
     */
    @Override
    public boolean equalsSaved(NBTTagCompound tag) {
        return true;
    }

    @Override
    public TrackerHolder getHolder() {
        return holder;
    }

    @Nonnull
    public TickLogger getTickLogger() throws TiqualityException.TrackerWasNotProfilingException {
        if(isProfiling == false){
            throw new TiqualityException.TrackerWasNotProfilingException(this);
        }else{
            return tickLogger;
        }
    }

    @Override
    public boolean equals(Object o){
        if(o == null || o instanceof ForcedTracker == false){
            return false;
        }else{
            if(o != this){
                throw new IllegalStateException("Detected two ForcedTracker objects, this is impossible. HALT");
            }else{
                return true;
            }
        }
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
