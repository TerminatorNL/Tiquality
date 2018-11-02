package cf.terminator.tiquality.integration.griefprevention;

import cf.terminator.tiquality.api.event.TiqualityEvent;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class SetTrackerInterceptor {

    public static final SetTrackerInterceptor INSTANCE = new SetTrackerInterceptor();

    private SetTrackerInterceptor(){

    }

    @SubscribeEvent
    public void onSetTracker(TiqualityEvent.SetBlockTrackerEvent e){
        ClaimManager claimManager = GriefPrevention.getApi().getClaimManager((World) e.getMinecraftWorld());
        Location<World> pos = new Location<>((World) e.getMinecraftWorld(), e.getPos().getX(), e.getPos().getY(), e.getPos().getZ());

        Claim claim = claimManager.getClaimAt(pos);
        if(claim.isWilderness()){
            return;
        }
        if(e.getTracker() instanceof GriefPreventionTracker){
            return;
        }
        GriefPreventionTracker tracker = GriefPreventionHook.findOrGetTrackerByClaim(claim);
        e.setTracker(tracker);
    }
}
