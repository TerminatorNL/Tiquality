package cf.terminator.tiquality.mixinhelper;

import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.tracking.ForcedTracker;
import cf.terminator.tiquality.tracking.TrackerBase;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

import static cf.terminator.tiquality.TiqualityConfig.QuickConfig.AUTO_WORLD_ASSIGNED_OBJECTS_FAST;

public class Hub {

    public static void onBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        TrackerBase tracker = ((TiqualityWorld) world).getTracker(pos);
        if(tracker != null) {
            tracker.doBlockTick(block,world, pos, state, rand);
        }else{
            if(AUTO_WORLD_ASSIGNED_OBJECTS_FAST.contains(block)){
                ForcedTracker.INSTANCE.doBlockTick(block, world, pos, state, rand);
            }
        }
    }

    public static void onRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        TrackerBase tracker = ((TiqualityWorld) world).getTracker(pos);
        if(tracker != null) {
            tracker.doRandomBlockTick(block,world, pos, state, rand);
        }else{
            if(AUTO_WORLD_ASSIGNED_OBJECTS_FAST.contains(block)){
                ForcedTracker.INSTANCE.doRandomBlockTick(block, world, pos, state, rand);
            }
        }
    }

    public static void onTileEntityTick(ITickable tickable){
        TileEntity entity = (TileEntity) tickable;
        TrackerBase tracker = ((TiqualityWorld)entity.getWorld()).getTracker(entity.getPos());
        if(tracker != null) {
            tracker.tickTileEntity((TiqualitySimpleTickable) tickable);
        }else{
            if(AUTO_WORLD_ASSIGNED_OBJECTS_FAST.contains(entity.getBlockType())){
                ForcedTracker.INSTANCE.tickTileEntity((TiqualitySimpleTickable) tickable);
            }
        }
    }

    public static void onEntityTick(Entity e){
        TiqualityEntity entity = (TiqualityEntity) e;
        TrackerBase tracker = entity.getTracker();
        if(tracker != null) {
            tracker.tickEntity(entity);
        }else{
            ForcedTracker.INSTANCE.tickEntity(entity);
        }
    }
}
