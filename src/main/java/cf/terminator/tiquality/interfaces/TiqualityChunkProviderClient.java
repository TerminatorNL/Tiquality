package cf.terminator.tiquality.interfaces;

import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;

public interface TiqualityChunkProviderClient {

    /**
     * We need to access the cache fast, and do not intend to load.
     * This is the only reason this exists.
     * @param key the key
     * @return the chunk, if it exists.
     */
    @Nullable Chunk getChunkFromCache(long key);
}
