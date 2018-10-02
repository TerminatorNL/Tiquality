package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.mixinhelper.Hub;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.event.tracking.TrackingUtil;

import java.util.Random;

/**
 * I need a better alternative than hooking into to the TrackingUtil class.
 * Profiler results on the TrackingUtil class will be completely wrong, because
 * sponge will not track the actual tickable it thinks to tick, but instead will
 * tick the associated PlayerTracker, which in turn can tick multiple unrelated blocks
 * or entities and may or may not include the intended tickable
 */
@Mixin(value = TrackingUtil.class, priority = 999)
public class MixinTrackingUtilSpongeWorkaround {

    @Redirect(method="updateTickBlock", at = @At(value = "INVOKE", target = "net/minecraft/block/Block.updateTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"))
    private static void onBlockTick(Block block, World worldIn, BlockPos pos, IBlockState state, Random rand){
        Hub.onBlockTick(block, worldIn, pos, state, rand);
    }

    @Redirect(method="randomTickBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;randomTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"))
    private static void onRandomBlockTick(Block block, World worldIn, BlockPos pos, IBlockState state, Random rand){
        Hub.onRandomBlockTick(block, worldIn, pos, state, rand);
    }

    @Redirect(method = "tickTileEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ITickable;update()V"))
    private static void onTileTick(ITickable tickable){
        Hub.onTileEntityTick(tickable);
    }

    @Redirect(method = "tickEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;onUpdate()V"))
    private static void onTileTick(Entity entity){
        Hub.onEntityTick(entity);
    }
}