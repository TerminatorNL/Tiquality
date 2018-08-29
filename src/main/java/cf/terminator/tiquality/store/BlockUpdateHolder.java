package cf.terminator.tiquality.store;

import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.interfaces.TiqualityWorldServer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class BlockUpdateHolder implements TiqualitySimpleTickable {

    private final Block block;
    private final World world;
    private final BlockPos pos;
    private final IBlockState state;
    private final Random rand;

    public BlockUpdateHolder(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        this.block = block;
        this.world = world;
        this.pos = pos;
        this.state = state;
        this.rand = rand;
    }

    /**
     * Method to actually run the reloadFromFile on the tickable.
     */
    @Override
    public void doUpdateTick() {
        TiqualityChunk chunk = ((TiqualityWorldServer) world).getChunkFast(pos);
        if(chunk != null && chunk.isChunkLoaded()) {
            block.updateTick(world, pos, state, rand);
        }
    }

    @Override
    public boolean equals(Object o) {
        if(o == null || o instanceof BlockUpdateHolder == false){
            return false;
        }
        BlockUpdateHolder other = (BlockUpdateHolder) o;
        return other.pos.equals(pos);
    }

    @Override
    public int hashCode(){
        return pos.hashCode();
    }
}
