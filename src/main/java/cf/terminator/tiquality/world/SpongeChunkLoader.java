package cf.terminator.tiquality.world;

import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.common.bridge.world.WorldInfoBridge;
import org.spongepowered.common.bridge.world.chunk.ServerChunkProviderBridge;
import org.spongepowered.common.config.category.WorldCategory;
import org.spongepowered.common.world.SpongeEmptyChunk;

import javax.annotation.Nonnull;

public class SpongeChunkLoader {

    /**
     * Temporarily disables SpongeForge's 'deny-chunk-requests' so we can store data.
     * @param world the world
     * @param pos a position anywhere in the chunk
     * @return the chunk
     */
    public static @Nonnull TiqualityChunk getChunkForced(TiqualityWorld world, BlockPos pos){
        if(world instanceof WorldServer){
            Chunk maybeFakeChunk = world.getMinecraftWorld().getChunk(pos);
            if(maybeFakeChunk instanceof SpongeEmptyChunk == false){
                return (TiqualityChunk) maybeFakeChunk;
            }

            ChunkProviderServer provider = ((WorldServer) world).getChunkProvider();
            if(provider instanceof ServerChunkProviderBridge){
                WorldCategory category = ((WorldInfoBridge) provider.world.getWorldInfo()).bridge$getConfigAdapter().getConfig().getWorld();
                boolean isDenying = category.getDenyChunkRequests();
                if(isDenying){
                    ((ServerChunkProviderBridge) provider).bridge$setDenyChunkRequests(false);
                    TiqualityChunk result = (TiqualityChunk) ((WorldServer) world).getChunk(pos);
                    ((ServerChunkProviderBridge) provider).bridge$setDenyChunkRequests(true);
                    return result;
                }else{
                    return (TiqualityChunk) provider.provideChunk(pos.getX() >> 4, pos.getZ() >> 4);
                }
            }
        }
        return (TiqualityChunk) world.getMinecraftWorld().getChunkProvider().provideChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }
}
