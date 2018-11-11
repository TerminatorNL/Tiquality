package cf.terminator.tiquality.world;

import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import org.spongepowered.common.interfaces.world.gen.IMixinChunkProviderServer;
import org.spongepowered.common.util.SpongeHooks;

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
            boolean isDenying = SpongeHooks.getActiveConfig(((WorldServer) world)).getConfig().getWorld().getDenyChunkRequests();
            WorldServer worldServer = (WorldServer) world;
            if(isDenying){
                IMixinChunkProviderServer provider = (IMixinChunkProviderServer) worldServer.getChunkProvider();
                provider.setDenyChunkRequests(false);
                TiqualityChunk result = world.getChunk(pos);
                provider.setDenyChunkRequests(true);
                return result;
            }else{
                return world.getChunk(pos);
            }
        }else{
            return world.getChunk(pos);
        }
    }
}