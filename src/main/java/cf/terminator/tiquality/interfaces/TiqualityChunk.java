package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.store.PlayerTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface TiqualityChunk extends Comparable<TiqualityChunk> {

    void lagGoggles_writeToNBT(NBTTagCompound tag);

    void lagGoggles_loadNBT(World world, NBTTagCompound tag);

    void lagGoggles_setTrackedPosition(BlockPos pos, PlayerTracker tracker);

    void lagGoggles_removeTracker(BlockPos pos);

    PlayerTracker lagGoggles_findTrackerByBlockPos(BlockPos pos);

    boolean isChunkLoaded();
}
