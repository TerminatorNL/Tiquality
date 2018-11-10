package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.tracking.TrackerBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public interface TiqualityChunk extends Comparable<TiqualityChunk> {

    void tiquality_writeToNBT(NBTTagCompound tag);

    void tiquality_loadNBT(World world, NBTTagCompound tag);

    /**
     * Sets a tracked position.
     * @param pos the position
     * @param tracker the tracker, or null if the tracker should be removed.
     */
    void tiquality_setTrackedPosition(BlockPos pos, TrackerBase tracker);

    /**
     * Sets a tracker for the entire chunk at once
     * @param tracker the tracker, or null if all trackers should be removed.
     */
    void tiquality_setTrackerForEntireChunk(TrackerBase tracker);

    TrackerBase tiquality_findTrackerByBlockPos(BlockPos pos);

    boolean isChunkLoaded();

    Chunk getMinecraftChunk();

    void associateTrackers();
}