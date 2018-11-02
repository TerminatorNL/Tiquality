package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A class used to Track unowned updates for profiling purposes.
 * This Tracker does not throttle it's tickables.
 */
public class ForcedTracker extends TrackerBase {

    public static final ForcedTracker INSTANCE = new ForcedTracker();

    private ForcedTracker() {
    }

    /**
     * Gets the NBT data from this object, is called when the tracker is saved to disk.
     */
    @Override
    public NBTTagCompound getNBT() {
        return new NBTTagCompound();
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
            tickable.doUpdateTick();
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
            block.updateTick(world, pos, state, rand);
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
            block.randomTick(world, pos, state, rand);
        }
    }

    /**
     * Gets the associated players for this tracker
     *
     * @return an empty list
     */
    @Nonnull
    @Override
    public List<GameProfile> getAssociatedPlayers() {
        return new ArrayList<>();
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
            entity.doUpdateTick();
        }
    }

    /**
     * Since we're a Tracker without an owner, we assign 0 time to it's tick time.
     * @param cache The current online player cache
     * @return 0
     */
    @Override
    public double getMultiplier(final GameProfile[] cache){
        return 0D;
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
     * @return the info describing this Tracker (Like the owner)
     */
    @Nonnull
    @Override
    public TextComponentString getInfo() {
        return new TextComponentString(TextFormatting.LIGHT_PURPLE + "Forced");
    }

    /**
     * @return an unique identifier for this Tracker CLASS TYPE, used to re-instantiate the tracker later on.
     * This should just return a hardcoded string.
     */
    @Nonnull
    public String getIdentifier() {
        return "forced";
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
