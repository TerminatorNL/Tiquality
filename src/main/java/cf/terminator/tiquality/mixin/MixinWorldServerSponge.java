package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.interfaces.TickExecutor;
import cf.terminator.tiquality.mixinhelper.extended.DynamicMethodFinder;
import cf.terminator.tiquality.mixinhelper.extended.MethodHeadInserter;
import cf.terminator.tiquality.tracking.TickHub;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.bridge.world.ServerWorldBridge;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.mixin.core.world.WorldServerMixin;

import java.util.Random;

@SuppressWarnings({"ReferenceToMixin"})
@Mixin(value = WorldServer.class, priority = 3000)
public abstract class MixinWorldServerSponge extends World implements TickExecutor {

    private boolean IS_CONTROLLED_BY_TIQUALITY;

    protected MixinWorldServerSponge(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn, Profiler profilerIn, boolean client) {
        super(saveHandlerIn, info, providerIn, profilerIn, client);
        throw new RuntimeException("Mixins cannot be instantiated");
    }

    /*
        ENTITY
     */
    @Dynamic(value = "onCallEntityUpdate is added by SpongeForge (redirect$onCallEntityUpdate$something)", mixin = WorldServerMixin.class)
    @DynamicMethodFinder.FindMethod(nameRegex = "redirect\\$onCallEntityUpdate")
    protected abstract void onCallEntityUpdate_Sponge(Entity entity);


    @MethodHeadInserter.InsertHead(nameRegex = "redirect\\$onCallEntityUpdate")
    @Dynamic(value = "onCallEntityUpdate is modified by SpongeForge", mixin = WorldServerMixin.class)
    private void onCallEntityUpdate(Entity entity){
        if(IS_CONTROLLED_BY_TIQUALITY == false){
            TickHub.onEntityTick(entity);
            /*
                THIS IS ACTUALLY VERY IMPORTANT.
                We inject this piece of code at the top of another method!
             */
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    @Override
    public void onEntityTick(Entity e) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        this.onCallEntityUpdate_Sponge(e);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }

    /*
        TILE ENTITY
     */
    @Dynamic(value = "onUpdateTileEntities is added by SpongeForge (redirect$onUpdateTileEntities$something)", mixin = WorldServerMixin.class)
    @DynamicMethodFinder.FindMethod(nameRegex = "redirect\\$onUpdateTileEntities")
    protected abstract void onUpdateTileEntities_Sponge(ITickable tickable);

    @MethodHeadInserter.InsertHead(nameRegex = "redirect\\$onUpdateTileEntities")
    @Dynamic(value = "onUpdateTileEntities is modified by SpongeForge", mixin = WorldServerMixin.class)
    private void onUpdateTileEntities(ITickable tickable){
        if(IS_CONTROLLED_BY_TIQUALITY == false){
            TickHub.onTileEntityTick(tickable);
            /*
                THIS IS ACTUALLY VERY IMPORTANT.
                We inject this piece of code at the top of another method!
             */
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    @Override
    public void onTileEntityTick(ITickable tickable) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        this.onUpdateTileEntities_Sponge(tickable);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }

    /*
        BLOCK
     */
    @MethodHeadInserter.InsertHead(nameRegex = "redirect\\$onUpdateTick")
    @Dynamic(value = "onUpdateTick is redirected by SpongeForge", mixin = WorldServerMixin.class)
    private void onUpdateTick(Block block, net.minecraft.world.World worldIn, BlockPos pos, IBlockState state, Random rand){
        if(IS_CONTROLLED_BY_TIQUALITY == false){
            TickHub.onBlockTick(block, worldIn, pos, state, rand);
            /*
                THIS IS ACTUALLY VERY IMPORTANT.
                We inject this piece of code at the top of another method!
             */
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    @Override
    public void onBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        TrackingUtil.updateTickBlock((ServerWorldBridge) world, block, pos, state, rand);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }

    /*
        RANDOM BLOCK
    */
    @Redirect(method = "updateBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;randomTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"))
    private void onRandomBlockTick_Minecraft(Block block, net.minecraft.world.World worldIn, BlockPos pos, IBlockState state, Random rand){
        if(IS_CONTROLLED_BY_TIQUALITY == false){
            TickHub.onRandomBlockTick(block, worldIn, pos, state, rand);
        }
    }

    @Dynamic(value = "updateBlocks is modified by SpongeForge", mixin = WorldServerMixin.class)
    @Redirect(method = "updateBlocks", at = @At(value = "INVOKE", target = "Lorg/spongepowered/common/event/tracking/TrackingUtil;randomTickBlock(Lorg/spongepowered/common/bridge/world/ServerWorldBridge;Lnet/minecraft/block/Block;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"), require = 1)
    private void onRandomBlockTick_TickHub(ServerWorldBridge mixinWorld, Block block, BlockPos pos, IBlockState state, Random rand){
        if(IS_CONTROLLED_BY_TIQUALITY == false){
            TickHub.onRandomBlockTick(block, (World) mixinWorld, pos, state, rand);
        }
    }

    @Override
    public void onRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        TrackingUtil.randomTickBlock((ServerWorldBridge) world, block, pos, state, rand);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }
}
