package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.interfaces.TiqualityChunk;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings({"ReferenceToMixin"})
@Mixin(value = Chunk.class, priority = 2000)
public abstract class MixinSpongeChunk implements TiqualityChunk {

    @Dynamic(value = "setBlockState is added by SpongeForge", mixin = MixinChunk.class)
    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/block/state/IBlockState;Lorg/spongepowered/api/world/BlockChangeFlag;)Lnet/minecraft/block/state/IBlockState;", at = @At("RETURN"), require = 1, remap = false)
    private void onSetBlockModifiedFlag(BlockPos pos, IBlockState newState, IBlockState currentState, BlockChangeFlag flag, CallbackInfoReturnable<IBlockState> cir){
        this.onSetBlockStateHook(pos, cir.getReturnValue());
    }
}
