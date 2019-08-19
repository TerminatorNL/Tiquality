package com.github.terminatornl.tiquality.world;

import com.github.terminatornl.tiquality.Tiquality;
import com.github.terminatornl.tiquality.interfaces.TiqualityChunk;
import com.github.terminatornl.tiquality.interfaces.TiqualityWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.common.bridge.world.WorldInfoBridge;
import org.spongepowered.common.bridge.world.chunk.ChunkBridge;
import org.spongepowered.common.config.category.WorldCategory;
import org.spongepowered.common.world.SpongeEmptyChunk;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;

public class SpongeChunkLoader {

    /**
     * Temporarily disables SpongeForge's 'deny-chunk-requests' so we can store data.
     *
     * @param world the world
     * @param pos the chunk position
     * @return the chunk
     */
    public static @Nonnull
    TiqualityChunk getChunkForced(TiqualityWorld world, ChunkPos pos) {
        if (world instanceof WorldServer) {
            Chunk maybeFakeChunk = world.getMinecraftWorld().getChunk(pos.x, pos.z);
            if (maybeFakeChunk instanceof SpongeEmptyChunk == false) {
                return (TiqualityChunk) maybeFakeChunk;
            }

            ChunkProviderServer provider = ((WorldServer) world).getChunkProvider();
            if (provider instanceof ChunkBridge) {
                WorldCategory category = ((WorldInfoBridge) provider.world.getWorldInfo()).bridge$getConfigAdapter().getConfig().getWorld();
                boolean isDenying = category.getDenyChunkRequests();
                if (isDenying) {
                    TiqualityChunk result;
                    try {
                        Field denyChunkRequestsField = WorldCategory.class.getDeclaredField("denyChunkRequests");
                        denyChunkRequestsField.set(category, false);
                        result = (TiqualityChunk) ((WorldServer) world).getChunk(pos.x, pos.z);
                        denyChunkRequestsField.set(category, true);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        Tiquality.LOGGER.warn("Unable to set deny chunk requests temporarily! Undefined behavior ahead...");
                        e.printStackTrace();
                        result = (TiqualityChunk) ((WorldServer) world).getChunk(pos.x, pos.z);
                    }
                    return result;
                } else {
                    return (TiqualityChunk) provider.provideChunk(pos.x >> 4, pos.z >> 4);
                }
            }
        }
        return (TiqualityChunk) world.getMinecraftWorld().getChunkProvider().provideChunk(pos.x >> 4, pos.z >> 4);
    }
}
