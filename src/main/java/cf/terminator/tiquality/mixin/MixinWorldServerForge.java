package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.tracking.TickHub;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(value = WorldServer.class, priority = 999)
public abstract class MixinWorldServerForge {

    @Redirect(method="tickUpdates", at = @At(value = "INVOKE", target = "net/minecraft/block/Block.updateTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"), require = 1)
    private void onBlockTick_tick(Block block, World worldIn, BlockPos pos, IBlockState state, Random rand){
        TickHub.onBlockTick(block, worldIn, pos, state, rand);
    }

    @Redirect(method="updateBlockTick", at = @At(value = "INVOKE", target = "net/minecraft/block/Block.updateTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"), require = 1)
    private void onBlockTick(Block block, World worldIn, BlockPos pos, IBlockState state, Random rand){
        TickHub.onBlockTick(block, worldIn, pos, state, rand);
    }

    @Redirect(method="updateBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;randomTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"), require = 1)
    private void onRandomBlockTick(Block block, World worldIn, BlockPos pos, IBlockState state, Random rand){
        TickHub.onRandomBlockTick(block, worldIn, pos, state, rand);
    }

}
