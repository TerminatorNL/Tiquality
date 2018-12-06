package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.TrackerAlreadyExistsException;
import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.interfaces.Tracker;
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
import javax.annotation.Nullable;
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

    private ForcedTracker() {
    }

    /**
     * Gets the NBT data from this object, is called when the tracker is saved to disk.
     */
    @Override
    public NBTTagCompound getNBT() {
        throw new UnsupportedOperationException("Tried to save ForcedTracker!");
    }

    @Override
    public TickLogger getTickLogger() {
        return tickLogger;
    }

    @Override
    public void setProfileEnabled(boolean shouldProfile) {
        Tiquality.SCHEDULER.scheduleWait(new Runnable() {
            @Override
            public void run() {
                if(isProfiling != shouldProfile) {
                    isProfiling = shouldProfile;
                    if(shouldProfile == false){
                        MinecraftForge.EVENT_BUS.post(new TiqualityEvent.ProfileCompletedEvent(ForcedTracker.this, getTickLogger()));
                    }else{
                        tickLogger.reset();
                    }
                }
            }
        });
    }

    @Nullable
    @Override
    public TickLogger stopProfiler() {
        return SynchronizedAction.run(new SynchronizedAction.Action<TickLogger>() {
            @Override
            public void run(SynchronizedAction.DynamicVar<TickLogger> variable) {
                if(isProfiling == true) {
                    isProfiling = false;
                    MinecraftForge.EVENT_BUS.post(new TiqualityEvent.ProfileCompletedEvent(ForcedTracker.this, getTickLogger()));
                    variable.set(getTickLogger());
                }
            }
        });
    }

    @Override
    public void setNextTickTime(long granted_ns) {

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
     * @param tickable the TiqualitySimpleTickable object (Tile Entities are castable.)
     */
    @Override
    public void tickTileEntity(TiqualitySimpleTickable tickable){
        if(isProfiling) {
            long start = System.nanoTime();
            tickable.doUpdateTick();
            long elapsed = System.nanoTime() - start;
            tickLogger.addNanosAndIncrementCalls(tickable.getLocation(), elapsed);
        }else{
            Tiquality.TICK_EXECUTOR.onTileEntityTick((ITickable) tickable);
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
            block.updateTick(world, pos, state, rand);
            long elapsed = System.nanoTime() - start;
            tickLogger.addNanosAndIncrementCalls(new TickLogger.Location(world, pos), elapsed);
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
            block.randomTick(world, pos, state, rand);
            long elapsed = System.nanoTime() - start;
            tickLogger.addNanosAndIncrementCalls(new TickLogger.Location(world, pos), elapsed);
        }else{
            Tiquality.TICK_EXECUTOR.onRandomBlockTick(block, world, pos, state, rand);
        }
    }

    @Override
    public void grantTick() {
        throw new UnsupportedOperationException("ForcedTracker does not need ticks");
    }

    @Override
    public void associateChunk(TiqualityChunk chunk) {
        throw new UnsupportedOperationException("ForcedTracker should not be associated to chunks");
    }

    @Override
    public void associateDelegatingTracker(Tracker tracker) {
        throw new UnsupportedOperationException("ForcedTracker should not be delegated");
    }

    /**
     * Gets the associated players for this tracker
     *
     * @return an empty list
     */
    @Nonnull
    @Override
    public List<GameProfile> getAssociatedPlayers() {
        throw new UnsupportedOperationException("Tried to get the associatedPlayers for a ForcedTracker");
    }

    @Override
    public boolean isDone() {
        return true;
    }

    /**
     * Ticks the entity, and optionally profiles it
     * @param entity the Entity to tick
     */
    @Override
    public void tickEntity(TiqualityEntity entity){
        if(isProfiling) {
            long start = System.nanoTime();
            entity.doUpdateTick();
            long elapsed = System.nanoTime() - start;
            tickLogger.addNanosAndIncrementCalls(entity.getLocation(), elapsed);
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

    /**
     * @return true, this tracker must not unload.
     */
    @Override
    public boolean isLoaded(){
        return true;
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

    @Override
    public boolean shouldUnload() {
        return false;
    }

    @Override
    public void onUnload() {
        throw new UnsupportedOperationException("Unloading ForcedTracker is never allowed.");
    }

    @Override
    public int compareTo(@Nonnull Object o) {
        return 0;
    }

    @Override
    public void checkColission(@Nonnull Tracker tracker) throws TrackerAlreadyExistsException {
        if(this.equals(tracker)){
            throw new TrackerAlreadyExistsException(this, tracker);
        }
    }

    private TrackerHolder holder = null;

    @Override
    public void setHolder(TrackerHolder holder) {
        this.holder = holder;
    }

    @Override
    public TrackerHolder getHolder() {
        return holder;
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
