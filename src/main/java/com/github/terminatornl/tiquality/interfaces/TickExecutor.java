package com.github.terminatornl.tiquality.interfaces;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * A class that it's sole purpose is to execute the tick. No tracking is done here.
 * This is used to co-exist with Sponge's tracking system.
 */
public interface TickExecutor {
    void onBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand);

    void onRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand);

    void onTileEntityTick(ITickable tickable);

    void onEntityTick(Entity e);
}
