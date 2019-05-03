package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.interfaces.TickExecutor;
import cf.terminator.tiquality.tracking.TickHub;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import org.spongepowered.common.mixin.core.world.MixinWorldServer;

import java.util.Random;

@SuppressWarnings({"ShadowTarget", "ReferenceToMixin"})
@Debug(export = true, print = true)
@Mixin(value = WorldServer.class, priority = 3000, remap = false)
public abstract class MixinWorldServerSponge implements TickExecutor {

    private boolean IS_CONTROLLED_BY_TIQUALITY;

    /*
        ENTITY
     */
    @Dynamic(value = "onCallEntityUpdate is added by SpongeForge (redirect$onCallEntityUpdate$zmd000)", mixin = MixinWorldServer.class)
    @Shadow protected abstract void redirect$onCallEntityUpdate$zmd000(Entity entity);

    @Dynamic(value = "onCallEntityUpdate is added by SpongeForge (redirect$onCallEntityUpdate$zmd000)", mixin = MixinWorldServer.class)
    @Inject(method = "redirect$onCallEntityUpdate$zmd000", at = @At("HEAD"), cancellable = true)
    private void onCallEntityUpdate(Entity entity, CallbackInfo ci){
        if(IS_CONTROLLED_BY_TIQUALITY == false){
            TickHub.onEntityTick(entity);
            ci.cancel();
        }
    }

    @Override
    public void onEntityTick(Entity e) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        this.redirect$onCallEntityUpdate$zmd000(e);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }

    /*
        TILE ENTITY
     */
    @Dynamic(value = "onUpdateTileEntities is added by SpongeForge (redirect$onUpdateTileEntities$zmd000)", mixin = MixinWorldServer.class)
    @Shadow protected abstract void redirect$onUpdateTileEntities$zmd000(ITickable tickable);

    @Dynamic(value = "onUpdateTileEntities is added by SpongeForge (redirect$onUpdateTileEntities$zmd000)", mixin = MixinWorldServer.class)
    @Inject(method = "redirect$onUpdateTileEntities$zmd000", at = @At("HEAD"), cancellable = true)
    private void onUpdateTileEntities(ITickable tickable, CallbackInfo ci){
        if(IS_CONTROLLED_BY_TIQUALITY == false){
            TickHub.onTileEntityTick(tickable);
            ci.cancel();
        }
    }

    @Override
    public void onTileEntityTick(ITickable tickable) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        this.redirect$onUpdateTileEntities$zmd000(tickable);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }

    /*
        BLOCK
     */
    @Dynamic(value = "onUpdateTick is added by SpongeForge (redirect$onUpdateTick$zme000)", mixin = MixinWorldServer.class)
    @Inject(method = "redirect$onUpdateTick$zme000", at = @At("HEAD"), cancellable = true)
    private void onUpdateTick(Block block, net.minecraft.world.World worldIn, BlockPos pos, IBlockState state, Random rand, CallbackInfo ci){
        if(IS_CONTROLLED_BY_TIQUALITY == false){
            TickHub.onBlockTick(block, worldIn, pos, state, rand);
            ci.cancel();
        }
    }

    @Override
    public void onBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        TrackingUtil.updateTickBlock((IMixinWorldServer) world, block, pos, state, rand);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }

    /*
        RANDOM BLOCK
    */
    @Dynamic(value = "onUpdateTick is added by SpongeForge (redirect$onUpdateTick$zme000)", mixin = MixinWorldServer.class)
    @Redirect(method = "updateBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;randomTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"))
    private void onRandomBlockTick_Minecraft(Block block, net.minecraft.world.World worldIn, BlockPos pos, IBlockState state, Random rand){
        if(IS_CONTROLLED_BY_TIQUALITY == false){
            TickHub.onRandomBlockTick(block, worldIn, pos, state, rand);
        }
    }

    @Dynamic(value = "onUpdateTick is added by SpongeForge (redirect$onUpdateTick$zme000)", mixin = MixinWorldServer.class)
    @Redirect(method = "updateBlocks", at = @At(value = "INVOKE", target = "Lorg/spongepowered/common/event/tracking/TrackingUtil;randomTickBlock(Lorg/spongepowered/common/interfaces/world/IMixinWorldServer;Lnet/minecraft/block/Block;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"))
    private void onRandomBlockTick_TickHub(IMixinWorldServer mixinWorld, Block block, BlockPos pos, IBlockState state, Random rand){
        if(IS_CONTROLLED_BY_TIQUALITY == false){
            TickHub.onRandomBlockTick(block, (World) mixinWorld, pos, state, rand);
        }
    }

    @Override
    public void onRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        TrackingUtil.randomTickBlock((IMixinWorldServer) world, block, pos, state, rand);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }
}
