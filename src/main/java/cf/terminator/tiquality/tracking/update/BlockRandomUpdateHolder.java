package cf.terminator.tiquality.tracking.update;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.profiling.ReferencedTickable;
import cf.terminator.tiquality.tracking.UpdateType;
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
        if(o == null || o instanceof BlockRandomUpdateHolder == false){
            return false;
        }
        BlockRandomUpdateHolder other = (BlockRandomUpdateHolder) o;
        return other.pos.equals(pos);
    }

    @Override
    public int hashCode(){
        return pos.hashCode();
    }

    @Override
    public void setUpdateType(@Nonnull UpdateType type) {
        updateType = type;
    }

    @Nonnull
    @Override
    public UpdateType getUpdateType() {
        return updateType;
    }
}
