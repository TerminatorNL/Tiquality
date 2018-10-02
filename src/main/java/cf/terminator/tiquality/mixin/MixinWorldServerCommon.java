package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.store.PlayerTracker;
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
    public @Nullable TiqualityChunk getChunkFast(BlockPos pos){
        return (TiqualityChunk) ((ChunkProviderServer)chunkProvider).id2ChunkMap.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
    }

    /**
     * Optimized way of getting the PlayerTracker using a BlockPos.
     * Don't forget PlayerTrackers reside inside chunks, so it still has to grab the chunk.
     * If you need to use the chunk later on, this is not for you.
     *
     * @param pos the position of the block
     * @return the chunk
     */
    public @Nullable PlayerTracker getPlayerTracker(BlockPos pos){
        TiqualityChunk chunk = (TiqualityChunk) ((ChunkProviderServer)chunkProvider).id2ChunkMap.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        return chunk == null ? null : chunk.lagGoggles_findTrackerByBlockPos(pos);
    }

    /**
     * Optimized way of setting the PlayerTracker using a BlockPos.
     * Don't forget PlayerTrackers reside inside chunks, so it still has to grab the chunk.
     * If you need to use the chunk later on, this is not for you.
     *
     * @param pos the position of the block
     * @param tracker the PlayerTracker to append.
     */
    public void setPlayerTracker(BlockPos pos, PlayerTracker tracker){
        TiqualityChunk chunk = (TiqualityChunk) ((ChunkProviderServer)chunkProvider).id2ChunkMap.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        if(chunk != null){
            chunk.tiquality_setTrackedPosition(pos, tracker);
        }
    }

}
