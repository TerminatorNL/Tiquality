package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;

/**
 * Converts Sponges' calls through Tiquality
 */
@Mixin(value = PhaseTracker.class, priority = 999, remap = false)
public class MixinSpongePhaseTracker {

    @Inject(method="setBlockState", at = @At("HEAD"), require = 1)
    private void onBlockTick(IMixinWorldServer mixinWorld, BlockPos pos, IBlockState newState, BlockChangeFlag flag, CallbackInfoReturnable<Boolean> cir){
        Tracker tracker = ((TiqualityWorld) mixinWorld).getTiqualityTracker(pos);
        if(tracker != null){
            tracker.notifyBlockStateChange((TiqualityWorld) mixinWorld, pos, newState);
        }
    }

}