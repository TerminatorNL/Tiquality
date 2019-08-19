package com.github.terminatornl.tiquality.integration.griefprevention;

import com.github.terminatornl.tiquality.api.event.TiqualityEvent;
import com.github.terminatornl.tiquality.integration.griefprevention.event.GPClaimCreatedFullyEvent;
import com.github.terminatornl.tiquality.interfaces.Tracker;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimManager;
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
    public void onClaimCreate(GPClaimCreatedFullyEvent e) {
        Tracker tracker = GriefPreventionHook.findOrGetTrackerByClaim(e.getClaim());
        if (tracker != null) {
            GriefPreventionHook.setClaimTrackers(e.getClaim(), tracker, null, null);
        }
    }

    @SubscribeEvent
    public void onSetTracker(TiqualityEvent.SetBlockTrackerEvent e) {
        ClaimManager claimManager = GriefPrevention.getApi().getClaimManager((World) e.getMinecraftWorld());
        Location<World> pos = new Location<>((World) e.getMinecraftWorld(), e.getPos().getX(), e.getPos().getY(), e.getPos().getZ());
        Claim claim = claimManager.getClaimAt(pos);
        if (GriefPreventionHook.isValidClaim(claim) == false) {
            return;
        }
        Tracker tracker = GriefPreventionHook.findOrGetTrackerByClaim(claim);
        e.setTracker(tracker);
    }

    @SubscribeEvent
    public void onSetTracker(TiqualityEvent.SetChunkTrackerEvent e) {
        Chunk chunk = (Chunk) e.getChunk();
        ClaimManager claimManager = GriefPrevention.getApi().getClaimManager((World) chunk.getWorld());

        long chunkLong = ChunkPos.asLong(chunk.x, chunk.z);

        Set<Claim> claimSet = claimManager.getChunksToClaimsMap().get(chunkLong);
        if (claimSet == null) {
            /* There are no claims in this chunk. */
            return;
        }
        for (Claim claim : claimSet) {
            if (GriefPreventionHook.isValidClaim(claim)) {
                e.setPerBlockMode();
                return;
            }
        }
    }

    /*
    @SubscribeEvent
    public void onSpawn(EntityJoinWorldEvent e){
        ClaimManager manager = GriefPrevention.getApi().getClaimManager((World) e.getWorld());
        Location<World> pos = new Location<>((World) e.getWorld(), e.getEntity().posX, e.getEntity().posY, e.getEntity().posZ);
        Claim claim = manager.getClaimAt(pos);
        Tracker tracker = GriefPreventionHook.findOrGetTrackerByClaim(claim);
        if(tracker != null) {
            ((TiqualityEntity) e.getEntity()).setTracker(tracker);
        }
    }*/
}
