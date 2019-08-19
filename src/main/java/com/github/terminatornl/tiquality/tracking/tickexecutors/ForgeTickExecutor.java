package com.github.terminatornl.tiquality.tracking.tickexecutors;

import com.github.terminatornl.tiquality.interfaces.TickExecutor;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Forge has no crazy tracking system, we can simply delegate updates.
 */
public class ForgeTickExecutor implements TickExecutor {

    @Override
    public void onBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        block.updateTick(world, pos, state, rand);
    }

    @Override
    public void onRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        block.randomTick(world, pos, state, rand);
    }

    @Override
    public void onTileEntityTick(ITickable tickable) {
        tickable.update();
    }

    @Override
    public void onEntityTick(Entity e) {
        e.onUpdate();
    }
}
