package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

import static cf.terminator.tiquality.TiqualityConfig.QuickConfig.AUTO_WORLD_ASSIGNED_OBJECTS_FAST;

public class TickHub {

    public static void onBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        Tracker tracker = ((TiqualityWorld) world).getTiqualityTracker(pos);
        if(tracker != null) {
            tracker.doBlockTick(block,world, pos, state, rand);
        }else{
            if(AUTO_WORLD_ASSIGNED_OBJECTS_FAST.contains(block)){
                ForcedTracker.INSTANCE.doBlockTick(block, world, pos, state, rand);
            }
        }
    }

    public static void onRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        Tracker tracker = ((TiqualityWorld) world).getTiqualityTracker(pos);
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
        Tracker tracker = ((TiqualityWorld)entity.getWorld()).getTiqualityTracker(entity.getPos());
        if(tracker != null) {
            tracker.tickTileEntity((TiqualitySimpleTickable) entity);
        }else{
            if(AUTO_WORLD_ASSIGNED_OBJECTS_FAST.contains(entity.getBlockType())){
                ForcedTracker.INSTANCE.tickTileEntity((TiqualitySimpleTickable) tickable);
            }
        }
    }

    public static void onEntityTick(Entity e){
        TiqualityEntity entity = (TiqualityEntity) e;
        Tracker tracker = entity.getTracker();
        if(tracker != null) {
            tracker.tickEntity(entity);
        }else{
            ForcedTracker.INSTANCE.tickEntity(entity);
        }
    }
}
