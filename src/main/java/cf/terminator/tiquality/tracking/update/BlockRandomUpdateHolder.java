package cf.terminator.tiquality.tracking.update;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.tracking.TickLogger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class BlockRandomUpdateHolder implements TiqualitySimpleTickable {

    private final World world;
    private final BlockPos pos;
    private final Random rand;

    public BlockRandomUpdateHolder(World world, BlockPos pos, Random rand) {
        this.world = world;
        this.pos = pos;
        this.rand = rand;
    }

    /**
     * Method to actually run the update on the tickable.
     */
    @Override
    public void doUpdateTick() {
        TiqualityChunk chunk = ((TiqualityWorld) world).getTiqualityChunk(pos);
        if(chunk.isChunkLoaded()) {
            IBlockState state = chunk.getMinecraftChunk().getBlockState(pos);
            Tiquality.TICK_EXECUTOR.onRandomBlockTick(state.getBlock(), world, pos, state, rand);
        }
    }

    /**
     * Method to get the position of the object
     */
    @Override
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Method to get the world of the object
     */
    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public TickLogger.Location getLocation() {
        return new TickLogger.Location(world, pos);
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

    /**
     * Gets the type of this Tickable
     *
     * @return the type
     */
    @Override
    public TickType getType() {
        return TickType.BLOCK_RANDOM;
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
}
