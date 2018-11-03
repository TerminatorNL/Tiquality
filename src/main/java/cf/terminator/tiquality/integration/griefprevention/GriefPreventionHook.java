package cf.terminator.tiquality.integration.griefprevention;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.Tracking;
import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.tracking.TrackerBase;
import cf.terminator.tiquality.tracking.TrackerManager;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.event.*;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GriefPreventionHook {

    private static final CreateClaimHandler createClaimHandler = new CreateClaimHandler();
    private static final ChangeClaimEventHandler claimChangeHandler = new ChangeClaimEventHandler();
    private static final TransferClaimEventHandler transferClaimHandler = new TransferClaimEventHandler();
    private static final DeleteClaimEventHandler deleteClaimHandler = new DeleteClaimEventHandler();
    private static final UserAddTrustClaimEventHandler userAddTrustHandler = new UserAddTrustClaimEventHandler();
    private static final UserRemoveTrustClaimEventHandler userRemoveTrustHandler = new UserRemoveTrustClaimEventHandler();

    public static void loadClaimsForcibly(ICommandSender sender){
        Tiquality.LOGGER.info("Importing griefprevention claims...");

        List<Claim> list = new ArrayList<>();
        for (World world : Sponge.getServer().getWorlds()) {
            list.addAll(GriefPrevention.getApi().getClaimManager(world).getWorldClaims());
        }
        for(Claim claim : list){
            Text owner = claim.getOwnerName();
            net.minecraft.world.World world = (net.minecraft.world.World) claim.getWorld();
            Location<World> pos = claim.getLesserBoundaryCorner();


            String identifier =
                    (owner != null ? owner.toPlain() : "Unknown") + " at DIM=" +
                            (world != null ? String.valueOf(world.provider.getDimension()) : "Unknown") + " "
                            + (pos != null ? "X: " + pos.getBlockX() + " Z: " + pos.getBlockZ() : "unknown");


            Tiquality.LOGGER.info("Importing claim: " + identifier);
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Importing claim: " + identifier));
            findOrGetTrackerByClaim(claim).setBlockTrackers();
        }

        Tiquality.LOGGER.info("Importing griefprevention claims finished.");
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Importing claims finished."));
    }

    public static GriefPreventionTracker findOrGetTrackerByClaim(@Nonnull Claim claim){
        for(TrackerBase tracker : TrackerManager.getEntrySet()){
            if(tracker instanceof GriefPreventionTracker){
                GriefPreventionTracker gpTracker = (GriefPreventionTracker) tracker;
                if(gpTracker.doesClaimExists()){
                    if(gpTracker.claim.getUniqueId().equals(claim.getUniqueId())){
                        return gpTracker;
                    }
                }
            }
        }
        return TrackerManager.preventCopies(new GriefPreventionTracker(claim));
    }

    public static void init(){
        Sponge.getEventManager().registerListener(Tiquality.INSTANCE, CreateClaimEvent.class, createClaimHandler);
        Sponge.getEventManager().registerListener(Tiquality.INSTANCE, ChangeClaimEvent.class, claimChangeHandler);
        Sponge.getEventManager().registerListener(Tiquality.INSTANCE, TransferClaimEvent.class, transferClaimHandler);
        Sponge.getEventManager().registerListener(Tiquality.INSTANCE, DeleteClaimEvent.class, deleteClaimHandler);
        Sponge.getEventManager().registerListener(Tiquality.INSTANCE, UserTrustClaimEvent.Add.class, userAddTrustHandler);
        Sponge.getEventManager().registerListener(Tiquality.INSTANCE, UserTrustClaimEvent.Remove.class, userRemoveTrustHandler);

        Tracking.registerCustomTracker(new GriefPreventionTracker(null));
        MinecraftForge.EVENT_BUS.register(SetTrackerInterceptor.INSTANCE);
        MinecraftForge.EVENT_BUS.register(EntityEventHandler.INSTANCE);
    }

    private static class CreateClaimHandler implements EventListener<CreateClaimEvent>{
        @Override
        public void handle(@Nonnull CreateClaimEvent event) {

            for(Claim claim : event.getClaims()){
                GriefPreventionTracker tracker = findOrGetTrackerByClaim(claim);
                tracker.setBlockTrackers();
            }
        }
    }

    private static class ChangeClaimEventHandler implements EventListener<ChangeClaimEvent>{
        @Override
        public void handle(@Nonnull ChangeClaimEvent event) {
            for(Claim claim : event.getClaims()){
                GriefPreventionTracker tracker = findOrGetTrackerByClaim(claim);
                tracker.setBlockTrackers();
            }
        }
    }

    private static class TransferClaimEventHandler implements EventListener<TransferClaimEvent>{
        @Override
        public void handle(@Nonnull TransferClaimEvent event) {
            UUID newUUID = event.getNewOwner();
            for(Claim claim : event.getClaims()){
                GriefPreventionTracker tracker = findOrGetTrackerByClaim(claim);
                tracker.setOwner(newUUID);
            }
        }
    }

    private static class DeleteClaimEventHandler implements EventListener<DeleteClaimEvent>{
        @Override
        public void handle(@Nonnull DeleteClaimEvent event) {
            Object source = event.getSource();
            if(source instanceof EntityPlayer){
                EntityPlayer player = (EntityPlayer) source;
                player.sendMessage(new TextComponentString(TextFormatting.DARK_GRAY + "[Tiquality] Claim removal detected, trackers updated."));

                for(Claim claim : event.getClaims()){
                    GriefPreventionTracker tracker = findOrGetTrackerByClaim(claim);
                    tracker.replaceTracker(TrackerManager.getOrCreatePlayerTrackerByProfile(player.getGameProfile()));
                }
            }
        }
    }

    private static class UserAddTrustClaimEventHandler implements EventListener<UserTrustClaimEvent.Add>{
        @Override
        public void handle(@Nonnull UserTrustClaimEvent.Add event) {
            for(Claim claim : event.getClaims()){
                GriefPreventionTracker tracker = findOrGetTrackerByClaim(claim);
                tracker.updatePlayers();
            }
        }
    }

    private static class UserRemoveTrustClaimEventHandler implements EventListener<UserTrustClaimEvent.Remove>{
        @Override
        public void handle(@Nonnull UserTrustClaimEvent.Remove event) {
            for(Claim claim : event.getClaims()){
                GriefPreventionTracker tracker = findOrGetTrackerByClaim(claim);
                tracker.updatePlayers();
            }
        }
    }

    private static class BorderClaimEventHandler implements EventListener<BorderClaimEvent>{
        @Override
        public void handle(@Nonnull BorderClaimEvent event) {
            TiqualityEntity entity = (TiqualityEntity) event.getTargetEntity();

            Claim claim = event.getEnterClaim();
            if(claim.isWilderness()){
                entity.setTracker(null);
            }else{
                entity.setTracker(findOrGetTrackerByClaim(claim));
            }



        }
    }
}
