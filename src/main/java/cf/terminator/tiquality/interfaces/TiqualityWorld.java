package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.store.PlayerTracker;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public interface TiqualityWorld {

    /**
     * Optimized way of getting a chunk using a BlockPos
     * @param pos the position of the block
     * @return the chunk
     */
    @Nullable
    TiqualityChunk getChunkFast(BlockPos pos);

    /**
     * Optimized way of getting the PlayerTracker using a BlockPos.
     * Don't forget PlayerTrackers reside inside chunks, so it still has to grab the chunk.
     * If you need to use the chunk later on, this is not for you.
     *
     * @param pos the position of the block
     * @return the player tracker
     */
    @Nullable PlayerTracker getPlayerTracker(BlockPos pos);

    /**
     * Optimized way of setting the PlayerTracker using a BlockPos.
     * Don't forget PlayerTrackers reside inside chunks, so it still has to grab the chunk.
     * If you need to use the chunk later on, this is not for you.
     *
     * @param pos the position of the block
     * @param tracker the PlayerTracker
     */
    void setPlayerTracker(BlockPos pos, PlayerTracker tracker);
}
