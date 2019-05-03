package cf.terminator.tiquality.tracking.tickexecutors;

import cf.terminator.tiquality.interfaces.TickExecutor;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The reason the TickExecutor interface exists, is this very class.
 * This makes sure all ticks are executed in sponge's TrackingUtil class and prevents recursion using
 * the IS_CONTROLLED_BY_TIQUALITY boolean variable.
 *
 * This allows Sponge to gain fine grained control, whilst allowing Tiquality to function
 */
public class SpongeTickExecutor implements TickExecutor {

    @Override
    public void onBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        ((TickExecutor) world).onBlockTick(block, world, pos, state, rand);
    }

    @Override
    public void onRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        ((TickExecutor) world).onRandomBlockTick(block, world, pos, state, rand);
    }

    @Override
    public void onTileEntityTick(ITickable tickable) {
        ((TickExecutor) ((TileEntity) tickable).getWorld()).onTileEntityTick(tickable);
    }

    @Override
    public void onEntityTick(Entity e) {
        ((TickExecutor) e.getEntityWorld()).onEntityTick(e);
    }
}
