package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.tracking.TickHub;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;

import java.util.Random;

import static cf.terminator.tiquality.tracking.tickexecutors.SpongeTickExecutor.IS_CONTROLLED_BY_TIQUALITY;

/**
 * Converts Sponges' calls through Tiquality, and then routes them back here when needed.
 */
@Mixin(value = TrackingUtil.class, priority = 999, remap = false)
public class MixinSpongeTrackingUtil {

    @Inject(method="updateTickBlock", at = @At("HEAD"), cancellable = true)
    private static void onBlockTick(IMixinWorldServer mixinWorld, Block block, BlockPos pos, IBlockState state, Random random, CallbackInfo ci){
        if(IS_CONTROLLED_BY_TIQUALITY == false){
            TickHub.onBlockTick(block, (World) mixinWorld, pos, state, random);
            ci.cancel();
        }
    }


    @Inject(method="randomTickBlock", at = @At("HEAD"), cancellable = true)
    private static void onRandomBlockTick(PhaseTracker phaseTracker, IMixinWorldServer mixinWorld, Block block, BlockPos pos, IBlockState state, Random random, CallbackInfo ci){
        if(IS_CONTROLLED_BY_TIQUALITY == false){
            TickHub.onRandomBlockTick(block, (World) mixinWorld, pos, state, random);
            ci.cancel();
        }
    }

    @Inject(method="tickTileEntity", at = @At("HEAD"), cancellable = true)
    private static void onRandomBlockTick(IMixinWorldServer mixinWorldServer, ITickable tile, CallbackInfo ci){
        if(IS_CONTROLLED_BY_TIQUALITY == false){
            TickHub.onTileEntityTick(tile);
            ci.cancel();
        }
    }

    @Inject(method="tickEntity", at = @At("HEAD"), cancellable = true)
    private static void onRandomBlockTick(Entity entity, CallbackInfo ci){
        if(IS_CONTROLLED_BY_TIQUALITY == false){
            TickHub.onEntityTick(entity);
            ci.cancel();
        }
    }
}