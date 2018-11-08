package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.mixinhelper.WorldHelper;
import cf.terminator.tiquality.tracking.TrackerBase;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mixin(value = WorldServer.class, priority = 999)
public abstract class MixinWorldServerCommon extends World implements TiqualityWorld {

    protected MixinWorldServerCommon(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn, Profiler profilerIn, boolean client) {
        super(saveHandlerIn, info, providerIn, profilerIn, client);
        throw new RuntimeException("This should never run...");
    }

    /**
     * Optimized way of getting a chunk using a BlockPos
     * @param pos the position of the block
     * @return the chunk
     */
    public @Nonnull TiqualityChunk getChunk(BlockPos pos){
        TiqualityChunk chunk = (TiqualityChunk) ((ChunkProviderServer)chunkProvider).id2ChunkMap.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        return chunk != null ? chunk : (TiqualityChunk) chunkProvider.provideChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    /**
     * Optimized way of getting the Tracker using a BlockPos.
     * Don't forget Tracker reside inside chunks, so it still has to grab the chunk.
     * If you need to use the chunk later on, this is not for you.
     *
     * @param pos the position of the block
     * @return the chunk
     */
    public @Nullable TrackerBase getTracker(BlockPos pos){
        return getChunk(pos).tiquality_findTrackerByBlockPos(pos);
    }

    /**
     * Optimized way of setting the Tracker using a BlockPos.
     * Don't forget Tracker reside inside chunks, so it still has to grab the chunk.
     * If you need to use the chunk later on, this is not for you.
     *
     * @param pos the position of the block
     * @param tracker the Tracker to set.
     */
    public void setTracker(BlockPos pos, TrackerBase tracker){
        getChunk(pos).tiquality_setTrackedPosition(pos, tracker);
    }

    /**
     * Sets the tracker in a cuboid area
     * @param start start coord (All lower)
     * @param end end coord (All higher)
     * @param tracker the tracker to add
     * @param callback a task to run on completion. This will run in the main thread!
     */
    public void setTrackerCuboidAsync(BlockPos start, BlockPos end, TrackerBase tracker, Runnable callback){
        WorldHelper.setTrackerCuboid(this, start, end, tracker, callback);
    }
}
