package cf.terminator.tiquality.interfaces;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;

public interface TiqualityChunk extends Comparable<TiqualityChunk> {

    @Nullable NBTTagCompound tiquality_getNBT();

    void tiquality_loadNBT(World world, NBTTagCompound tag);

    /**
     * Sets a tracked position.
     * @param pos the position
     * @param tracker the tracker, or null if the tracker should be removed.
     */
    void tiquality_setTrackedPosition(BlockPos pos, Tracker tracker);

    /**
     * Sets a tracker for the entire chunk at once
     * @param tracker the tracker, or null if all trackers should be removed.
     */
    void tiquality_setTrackerForEntireChunk(Tracker tracker);

    Tracker tiquality_findTrackerByBlockPos(BlockPos pos);

    boolean isChunkLoaded();

    Chunk getMinecraftChunk();

    void associateTrackers();

    void replaceTracker(Tracker oldTracker, Tracker newTracker);

    void tiquality_mark(BlockPos pos);

    void tiquality_unMark(BlockPos pos);

    boolean tiquality_isMarked(BlockPos pos);
}