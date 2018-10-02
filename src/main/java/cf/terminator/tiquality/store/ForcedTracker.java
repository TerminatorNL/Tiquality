package cf.terminator.tiquality.store;

import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;
import java.util.UUID;

/**
 * A class used to Track unowned updates for profiling purposes.
 * This Tracker does not throttle it's tickables.
 */
public class ForcedTracker extends PlayerTracker {

    public static ForcedTracker INSTANCE = new ForcedTracker();

    private ForcedTracker() {
        super(new GameProfile(new UUID(9136712361234667432L, 1812357000566439086L), "[TiqualityForce]"));
    }

    /**
     * Checks if the owner is a fake owner.
     * Trackers belonging to fake owners are not removed and kept in memory.
     * This method is meant to be overridden.
     *
     * @return true if this is a fake owner.
     */
    @Override
    public boolean isFakeOwner(){
        return true;
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
        return "ForcedTracker:{Owner: '[TiqualityForce]', hashCode: " + System.identityHashCode(this) + "}";
    }

    @Override
    public boolean equals(Object o){
        if(o == null || o instanceof ForcedTracker == false){
            return false;
        }else{
            return o == this || this.getOwner().getId().equals(((ForcedTracker) o).getOwner().getId());
        }
    }
}
