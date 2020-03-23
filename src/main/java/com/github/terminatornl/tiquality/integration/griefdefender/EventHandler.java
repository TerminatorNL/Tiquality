package com.github.terminatornl.tiquality.integration.griefdefender;

import com.github.terminatornl.tiquality.api.event.TiqualityEvent;
import com.github.terminatornl.tiquality.integration.griefdefender.event.GDClaimCreatedFullyEvent;
import com.github.terminatornl.tiquality.interfaces.Tracker;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Set;

public class EventHandler {

    public static final EventHandler INSTANCE = new EventHandler();

    private EventHandler() {

    }

    @SubscribeEvent
    public void onClaimCreate(GDClaimCreatedFullyEvent e) {
        Tracker tracker = GriefDefenderHook.findOrGetTrackerByClaim(e.getClaim());
        if (tracker != null) {
            GriefDefenderHook.setClaimTrackers(e.getClaim(), tracker, null, null);
        }
    }

    @SubscribeEvent
    public void onSetTracker(TiqualityEvent.SetBlockTrackerEvent e) {
        ClaimManager claimManager = GriefDefender.getCore().getClaimManager(((World) e.getMinecraftWorld()).getUniqueId());
        Location<World> pos = new Location<>((World) e.getMinecraftWorld(), e.getPos().getX(), e.getPos().getY(), e.getPos().getZ());
        Claim claim = claimManager.getClaimAt(pos.getBlockPosition());
        if (GriefDefenderHook.isValidClaim(claim) == false) {
            return;
        }
        Tracker tracker = GriefDefenderHook.findOrGetTrackerByClaim(claim);
        e.setTracker(tracker);
    }

    @SubscribeEvent
    public void onSetTracker(TiqualityEvent.SetChunkTrackerEvent e) {
        Chunk chunk = (Chunk) e.getChunk();
        ClaimManager claimManager = GriefDefender.getCore().getClaimManager(((World) chunk.getWorld()).getUniqueId());

        long chunkLong = ChunkPos.asLong(chunk.x, chunk.z);

        Set<Claim> claimSet = claimManager.getChunksToClaimsMap().get(chunkLong);
        if (claimSet == null) {
            /* There are no claims in this chunk. */
            return;
        }
        for (Claim claim : claimSet) {
            if (GriefDefenderHook.isValidClaim(claim)) {
                e.setPerBlockMode();
                return;
            }
        }
    }

    /*
    @SubscribeEvent
    public void onSpawn(EntityJoinWorldEvent e){
        ClaimManager claimManager = GriefDefender.getCore().getClaimManager(((World) e.getWorld()).getUniqueId());

        Location<World> pos = new Location<>((World) e.getWorld(), e.getEntity().posX, e.getEntity().posY, e.getEntity().posZ);
        Claim claim = manager.getClaimAt(pos.getBlockPosition());
        Tracker tracker = GriefDefenderHook.findOrGetTrackerByClaim(claim);
        if(tracker != null) {
            ((TiqualityEntity) e.getEntity()).setTracker(tracker);
        }
    }*/
}
