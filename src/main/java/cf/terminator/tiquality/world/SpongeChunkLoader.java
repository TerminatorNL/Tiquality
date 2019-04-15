package cf.terminator.tiquality.world;

import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import org.spongepowered.common.interfaces.world.gen.IMixinChunkProviderServer;
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
            IMixinWorldServer mixinWorldServer = (IMixinWorldServer) world;
            boolean isDenying = mixinWorldServer.getWorldConfig().getConfig().getWorld().getDenyChunkRequests();
            if(isDenying){
                IMixinChunkProviderServer provider = (IMixinChunkProviderServer) ((WorldServer) world).getChunkProvider();
                provider.setDenyChunkRequests(false);
                TiqualityChunk result = (TiqualityChunk) ((WorldServer) world).getChunk(pos);
                provider.setDenyChunkRequests(true);
                return result;
            }else{
                return (TiqualityChunk) world.getMinecraftWorld().getChunkProvider().provideChunk(pos.getX() >> 4, pos.getZ() >> 4);
            }
        }
        return (TiqualityChunk) world.getMinecraftWorld().getChunkProvider().provideChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }
}
