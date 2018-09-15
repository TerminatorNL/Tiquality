package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.store.PlayerTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface TiqualityChunk extends Comparable<TiqualityChunk> {

    void tiquality_writeToNBT(NBTTagCompound tag);

    void tiquality_loadNBT(World world, NBTTagCompound tag);

    void tiquality_setTrackedPosition(BlockPos pos, PlayerTracker tracker);

    void tiquality_removeTracker(BlockPos pos);

    PlayerTracker lagGoggles_findTrackerByBlockPos(BlockPos pos);

    boolean isChunkLoaded();
}