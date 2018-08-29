package cf.terminator.tiquality.store;

import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualityWorldServer;
import cf.terminator.tiquality.util.ForgeData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;

public class BlockTracker {

    /**
     * Sets the owner of a block
     * @param world The world object, must be of WorldServer type.
     * @param pos The block position
     * @param uuid The owner's UUID
     */
    public static void setOwner(World world, BlockPos pos, UUID uuid){
        TiqualityChunk chunk = ((TiqualityWorldServer) world).getChunkFast(pos);
        if(chunk != null) {
            chunk.lagGoggles_setTrackedPosition(pos, TrackerHub.getPlayerTrackerSafeByProfile(ForgeData.getGameProfileByUUID(uuid)));
        }
    }

    /**
     * Retrieves the PlayerTracker of that block.
     * This contains useful info like the owner and the time he/she has left.
     *
     * @param world the WorldServer
     * @param pos the block position
     * @return the PlayerTracker
     */
    public static @Nullable PlayerTracker getTracker(World world, BlockPos pos){
        return ((TiqualityWorldServer) world).getPlayerTracker(pos);
    }

}
