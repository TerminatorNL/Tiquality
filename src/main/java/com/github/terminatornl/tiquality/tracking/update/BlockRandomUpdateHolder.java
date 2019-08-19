package com.github.terminatornl.tiquality.tracking.update;

import com.github.terminatornl.tiquality.Tiquality;
import com.github.terminatornl.tiquality.interfaces.TiqualityChunk;
import com.github.terminatornl.tiquality.interfaces.TiqualitySimpleTickable;
import com.github.terminatornl.tiquality.interfaces.TiqualityWorld;
import com.github.terminatornl.tiquality.profiling.ReferencedTickable;
import com.github.terminatornl.tiquality.tracking.UpdateType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public class BlockRandomUpdateHolder implements TiqualitySimpleTickable {

    private final World world;
    private final BlockPos pos;
    private final Random rand;
    private UpdateType updateType;

    public BlockRandomUpdateHolder(World world, BlockPos pos, Random rand, UpdateType updateType) {
        this.world = world;
        this.pos = pos;
        this.rand = rand;
        this.updateType = updateType;
    }

    @Nonnull
    public static ReferencedTickable.BlockReference getId(int dim, BlockPos pos) {
        return new ReferencedTickable.BlockReference(dim, pos);
    }

    /**
     * Checks if this tickable is loaded, eg: chunk load status
     *
     * @return chunk status
     */
    @Override
    public boolean tiquality_isLoaded() {
        return world.isBlockLoaded(pos);
    }

    /**
     * Method to actually run the update on the tickable.
     */
    @Override
    public void tiquality_doUpdateTick() {
        TiqualityChunk chunk = ((TiqualityWorld) world).getTiqualityChunk(pos);
        IBlockState state = chunk.getMinecraftChunk().getBlockState(pos);
        Tiquality.TICK_EXECUTOR.onRandomBlockTick(state.getBlock(), world, pos, state, rand);
    }

    /**
     * Method to get the position of the object
     */
    @Override
    public BlockPos tiquality_getPos() {
        return pos;
    }

    /**
     * Method to get the world of the object
     */
    @Override
    public World tiquality_getWorld() {
        return world;
    }

    /**
     * Gets the reference to this tickable
     *
     * @return the reference
     */
    @Override
    @Nullable
    public ReferencedTickable.Reference getId() {
        return new ReferencedTickable.BlockReference(world.provider.getDimension(), pos);
    }

    @Override
    public void tiquality_mark() {
        ((TiqualityWorld) world).tiquality_mark(pos);
    }

    @Override
    public void tiquality_unMark() {
        ((TiqualityWorld) world).tiquality_unMark(pos);
    }

    @Override
    public boolean tiquality_isMarked() {
        return ((TiqualityWorld) world).tiquality_isMarked(pos);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BlockRandomUpdateHolder == false) {
            return false;
        }
        BlockRandomUpdateHolder other = (BlockRandomUpdateHolder) o;
        return other.pos.equals(pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }

    @Nonnull
    @Override
    public UpdateType getUpdateType() {
        return updateType;
    }

    @Override
    public void setUpdateType(@Nonnull UpdateType type) {
        updateType = type;
    }
}
