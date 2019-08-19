package com.github.terminatornl.tiquality.mixin;

import com.github.terminatornl.tiquality.interfaces.TickExecutor;
import com.github.terminatornl.tiquality.mixinhelper.extended.DynamicExclusion;
import com.github.terminatornl.tiquality.mixinhelper.extended.DynamicMethodFinder;
import com.github.terminatornl.tiquality.mixinhelper.extended.DynamicMethodRedirector;
import com.github.terminatornl.tiquality.mixinhelper.extended.MethodHeadInserter;
import com.github.terminatornl.tiquality.tracking.TickHub;
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
import org.spongepowered.common.bridge.world.WorldServerBridge;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.mixin.core.world.WorldServerMixin;

import java.util.Random;

@SuppressWarnings({"ReferenceToMixin"})
@Mixin(value = WorldServer.class)
public abstract class MixinWorldServerSponge extends World implements TickExecutor {

    private boolean IS_CONTROLLED_BY_TIQUALITY;

    protected MixinWorldServerSponge(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn, Profiler profilerIn, boolean client) {
        super(saveHandlerIn, info, providerIn, profilerIn, client);
        throw new RuntimeException("Mixins cannot be instantiated");
    }

    /*
        RANDOM BLOCK
    */
    @Dynamic(value = "updateBlocks is modified by SpongeForge", mixin = WorldServerMixin.class)
    @DynamicMethodRedirector.RedirectMethod(deobfRegexName = "randomTick", obfRegexName = "func_180645_a", deobfRegexOwner = "net/minecraft/block/Block", obfRegexOwner = "net/minecraft/block/Block")
    private static void onRandomBlockTick_Minecraft(Block block, net.minecraft.world.World worldIn, BlockPos pos, IBlockState state, Random rand) {
        TickHub.onRandomBlockTick(block, worldIn, pos, state, rand);
    }

    @Dynamic(value = "updateBlocks is modified by SpongeForge", mixin = WorldServerMixin.class)
    @DynamicMethodRedirector.RedirectMethod(deobfRegexName = "randomTickBlock", obfRegexName = "randomTickBlock", deobfRegexOwner = "org/spongepowered/common/event/tracking/TrackingUtil", obfRegexOwner = "org/spongepowered/common/event/tracking/TrackingUtil")
    private static void onRandomBlockTick_TrackingUtil(final WorldServerBridge mixinWorld, final Block block, final BlockPos pos, final IBlockState state, final Random random) {
        TickHub.onRandomBlockTick(block, (World) mixinWorld, pos, state, random);
    }

    @Override
    public void onEntityTick(Entity e) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        this.onCallEntityUpdate_Sponge(e);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }

    /*
        ENTITY
     */
    @Dynamic(value = "onCallEntityUpdate is added by SpongeForge (redirect$onCallEntityUpdate$something)", mixin = WorldServerMixin.class)
    @DynamicMethodFinder.FindMethod(deobfRegexName = "redirect\\$onCallEntityUpdate", obfRegexName = "redirect\\$onCallEntityUpdate")
    protected abstract void onCallEntityUpdate_Sponge(Entity entity);

    @MethodHeadInserter.InsertHead(deobfRegexName = "redirect\\$onCallEntityUpdate", obfRegexName = "redirect\\$onCallEntityUpdate")
    @Dynamic(value = "onCallEntityUpdate is modified by SpongeForge", mixin = WorldServerMixin.class)
    private void onCallEntityUpdate(Entity entity) {
        if (IS_CONTROLLED_BY_TIQUALITY == false) {
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
    public void onTileEntityTick(ITickable tickable) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        this.onUpdateTileEntities_Sponge(tickable);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }

    /*
        TILE ENTITY
     */
    @Dynamic(value = "onUpdateTileEntities is added by SpongeForge (redirect$onUpdateTileEntities$something)", mixin = WorldServerMixin.class)
    @DynamicMethodFinder.FindMethod(deobfRegexName = "redirect\\$onUpdateTileEntities", obfRegexName = "redirect\\$onUpdateTileEntities")
    protected abstract void onUpdateTileEntities_Sponge(ITickable tickable);

    @MethodHeadInserter.InsertHead(deobfRegexName = "redirect\\$onUpdateTileEntities", obfRegexName = "redirect\\$onUpdateTileEntities")
    @Dynamic(value = "onUpdateTileEntities is modified by SpongeForge", mixin = WorldServerMixin.class)
    private void onUpdateTileEntities(ITickable tickable) {
        if (IS_CONTROLLED_BY_TIQUALITY == false) {
            TickHub.onTileEntityTick(tickable);
            /*
                THIS IS ACTUALLY VERY IMPORTANT.
                We inject this piece of code at the top of another method!
             */
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    /*
        BLOCK
     */
    @MethodHeadInserter.InsertHead(deobfRegexName = "redirect\\$onUpdateTick", obfRegexName = "redirect\\$onUpdateTick")
    @Dynamic(value = "onUpdateTick is redirected by SpongeForge", mixin = WorldServerMixin.class)
    private void onUpdateTick(Block block, net.minecraft.world.World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (IS_CONTROLLED_BY_TIQUALITY == false) {
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
        TrackingUtil.updateTickBlock((WorldServerBridge) world, block, pos, state, rand);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }

    @Override
    @DynamicExclusion
    public void onRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        IS_CONTROLLED_BY_TIQUALITY = true;
        TrackingUtil.randomTickBlock((WorldServerBridge) world, block, pos, state, rand);
        IS_CONTROLLED_BY_TIQUALITY = false;
    }
}
