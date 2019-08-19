package com.github.terminatornl.tiquality.tracking;

import com.github.terminatornl.tiquality.interfaces.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class TickHub {

    public static void onBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        UpdateType updateType = ((UpdateTyped) block).getUpdateType();
        switch (updateType) {
            case DEFAULT:
            case PRIORITY:
            case NATURAL:
            case ALWAYS_TICK:
                Tracker tracker = ((TiqualityWorld) world).getTiqualityTracker(pos);
                if (tracker != null) {
                    tracker.doBlockTick(block, world, pos, state, rand);
                } else if (updateType == UpdateType.NATURAL || updateType == UpdateType.ALWAYS_TICK) {
                    ForcedTracker.INSTANCE.doBlockTick(block, world, pos, state, rand);
                }
                return;
            case TICK_DENIED:
                DenyTracker.INSTANCE.doBlockTick(block, world, pos, state, rand);
        }
    }

    public static void onRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        UpdateType updateType = ((UpdateTyped) block).getUpdateType();
        switch (updateType) {
            case DEFAULT:
            case PRIORITY:
            case NATURAL:
            case ALWAYS_TICK:
                Tracker tracker = ((TiqualityWorld) world).getTiqualityTracker(pos);
                if (tracker != null) {
                    tracker.doRandomBlockTick(block, world, pos, state, rand);
                } else if (updateType == UpdateType.NATURAL || updateType == UpdateType.ALWAYS_TICK) {
                    ForcedTracker.INSTANCE.doRandomBlockTick(block, world, pos, state, rand);
                }
                return;
            case TICK_DENIED:
                DenyTracker.INSTANCE.doRandomBlockTick(block, world, pos, state, rand);
        }
    }

    public static void onTileEntityTick(ITickable tickable) {
        TiqualitySimpleTickable simpleTickable = (TiqualitySimpleTickable) tickable;
        UpdateType updateType = simpleTickable.getUpdateType();
        switch (updateType) {
            case DEFAULT:
            case PRIORITY:
            case NATURAL:
            case ALWAYS_TICK:
                Tracker tracker = ((TiqualityWorld) simpleTickable.tiquality_getWorld()).getTiqualityTracker(simpleTickable.tiquality_getPos());
                if (tracker != null) {
                    tracker.tickSimpleTickable(simpleTickable);
                } else if (updateType == UpdateType.NATURAL || updateType == UpdateType.ALWAYS_TICK) {
                    ForcedTracker.INSTANCE.tickSimpleTickable(simpleTickable);
                }
                return;
            case TICK_DENIED:
                DenyTracker.INSTANCE.tickSimpleTickable(simpleTickable);
        }
    }

    public static void onEntityTick(Entity e) {
        TiqualityEntity entity = (TiqualityEntity) e;
        switch (entity.getUpdateType()) {
            case DEFAULT:
            case ALWAYS_TICK:
            case NATURAL:
            case PRIORITY:
                Tracker tracker = entity.getTracker();
                if (tracker != null) {
                    tracker.tickEntity(entity);
                } else {
                    ForcedTracker.INSTANCE.tickEntity(entity);
                }
                return;
            case TICK_DENIED:
                DenyTracker.INSTANCE.tickEntity(entity);
        }
    }
}
