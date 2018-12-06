package cf.terminator.tiquality.tracking.tickexecutors;

import cf.terminator.tiquality.interfaces.TickExecutor;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;

import java.util.Random;

/**
 * The reason the TickExecutor interface exists, is this very class.
 * This makes sure all ticks are executed in sponge's TrackingUtil class and prevents recursion using
 * the IS_CONTROLLED_BY_TIQUALITY boolean variable.
 *
 * This allows Sponge to gain fine grained control, whilst allowing Tiquality to function
 */
public class SpongeTickExecutor implements TickExecutor {

    public static boolean IS_CONTROLLED_BY_TIQUALITY = false;
    private static final PhaseTracker PHASE_TRACKER = PhaseTracker.getInstance();

    @Override
    public void onBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        TrackingUtil.updateTickBlock((IMixinWorldServer) world, block, pos, state, rand);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }

    @Override
    public void onRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        TrackingUtil.randomTickBlock(PHASE_TRACKER, (IMixinWorldServer) world, block, pos, state, rand);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }

    @Override
    public void onTileEntityTick(ITickable tickable) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        TrackingUtil.tickTileEntity((IMixinWorldServer) ((TileEntity) tickable).getWorld(), tickable);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }

    @Override
    public void onEntityTick(Entity e) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        TrackingUtil.tickEntity(e);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }
}
