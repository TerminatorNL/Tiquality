package cf.terminator.tiquality.integration.griefprevention;

import cf.terminator.tiquality.interfaces.TiqualityEntity;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class EntityEventHandler {

    public static final EntityEventHandler INSTANCE = new EntityEventHandler();

    private EntityEventHandler(){

    }

    @SubscribeEvent
    public void onSpawn(EntityJoinWorldEvent e){
        ClaimManager manager = GriefPrevention.getApi().getClaimManager((World) e.getWorld());
        Location<World> pos = new Location<>((World) e.getWorld(), e.getEntity().posX, e.getEntity().posY, e.getEntity().posZ);

        Claim claim = manager.getClaimAt(pos);
        if(claim.isWilderness()){
            return;
        }

        GriefPreventionTracker tracker = GriefPreventionHook.findOrGetTrackerByClaim(claim);
        ((TiqualityEntity) e.getEntity()).setTracker(tracker);
    }

}
