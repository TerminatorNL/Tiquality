package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.interfaces.TiqualityChunkProviderClient;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(value = ChunkProviderClient.class, priority = 999)
public abstract class MixinChunkProviderClient implements TiqualityChunkProviderClient {

    @Shadow @Final private Long2ObjectMap<Chunk> chunkMapping;

    /**
     * We need to access the cache fast, and do not intend to load any unloaded chunks.
     * This is the only reason this exists.
     */
    @Override
    public @Nullable Chunk getChunkFromCache(long key){
        return this.chunkMapping.get(key);
    }


}
