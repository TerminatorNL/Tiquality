package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.interfaces.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class TickHub {

    public static void onBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        Tracker tracker = ((TiqualityWorld) world).getTiqualityTracker(pos);
        if(tracker != null) {
            tracker.doBlockTick(block,world, pos, state, rand);
        }else{
            if(((TiqualityBlock) block).getUpdateType().mustTick(null)){
                ForcedTracker.INSTANCE.doBlockTick(block, world, pos, state, rand);
            }else{
                ((TiqualityWorld) world).setTiqualityTracker(pos, DenyTracker.INSTANCE);
            }
        }
    }

    public static void onRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand){
        Tracker tracker = ((TiqualityWorld) world).getTiqualityTracker(pos);
        if(tracker != null) {
            tracker.doRandomBlockTick(block,world, pos, state, rand);
        }else{
            if(((TiqualityBlock) block).getUpdateType().mustTick(null)){
                ForcedTracker.INSTANCE.doRandomBlockTick(block, world, pos, state, rand);
            }else{
                ((TiqualityWorld) world).setTiqualityTracker(pos, DenyTracker.INSTANCE);
            }
        }
    }

    public static void onTileEntityTick(ITickable tickable){
        TileEntity entity = (TileEntity) tickable;
        Tracker tracker = ((TiqualityWorld)entity.getWorld()).getTiqualityTracker(entity.getPos());
        if(tracker != null) {
            tracker.tickTileEntity((TiqualitySimpleTickable) entity);
        }else{
            if(((TiqualityBlock) entity.getBlockType()).getUpdateType().mustTick(null)){
                ForcedTracker.INSTANCE.tickTileEntity((TiqualitySimpleTickable) tickable);
            }else{
                ((TiqualityWorld)entity.getWorld()).setTiqualityTracker(entity.getPos(), DenyTracker.INSTANCE);
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
