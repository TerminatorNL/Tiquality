package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.tracking.TrackerBase;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface TiqualityWorld {

    /**
     * Optimized way of getting a chunk using a BlockPos
     * @param pos the position of the block
     * @return the chunk
     */
    @Nonnull TiqualityChunk getChunk(BlockPos pos);

    /**
     * Optimized way of getting the Tracker using a BlockPos.
     * Don't forget PlayerTrackers reside inside chunks, so it still has to grab the chunk.
     * If you need to use the chunk later on, this is not for you.
     *
     * @param pos the position of the block
     * @return the tracker
     */
    @Nullable TrackerBase getTracker(BlockPos pos);

    /**
     * Optimized way of setting the Tracker using a BlockPos.
     * Don't forget PlayerTrackers reside inside chunks, so it still has to grab the chunk.
     * If you need to use the chunk later on, this is not for you.
     *
     * @param pos the position of the block
     * @param tracker the Tracker
     */
    void setTracker(BlockPos pos, TrackerBase tracker);

    /**
     * Sets the tracker in a cuboid area
     * @param start start coord (All lower)
     * @param end end coord (All lower)
     * @param tracker the tracker to add
     * @param callback a task to run on completion
     */
    void setTrackerCuboidAsync(BlockPos start, BlockPos end, TrackerBase tracker, Runnable callback);
}
