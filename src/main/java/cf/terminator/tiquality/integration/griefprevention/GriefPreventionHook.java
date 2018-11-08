package cf.terminator.tiquality.integration.griefprevention;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.Tracking;
import cf.terminator.tiquality.integration.griefprevention.event.GPClaimCreatedFullyEvent;
import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.tracking.PlayerTracker;
import cf.terminator.tiquality.tracking.TrackerBase;
import cf.terminator.tiquality.tracking.TrackerManager;
import cf.terminator.tiquality.world.WorldHelper;
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
import java.util.concurrent.atomic.AtomicInteger;

public class GriefPreventionHook {

    private static final CreateClaimEventHandler createClaimHandler = new CreateClaimEventHandler();
    private static final ChangeClaimEventHandler claimChangeHandler = new ChangeClaimEventHandler();
    private static final TransferClaimEventHandler transferClaimHandler = new TransferClaimEventHandler();
    private static final DeleteClaimEventHandler deleteClaimHandler = new DeleteClaimEventHandler();
    private static final UserAddTrustClaimEventHandler userAddTrustHandler = new UserAddTrustClaimEventHandler();
    private static final UserRemoveTrustClaimEventHandler userRemoveTrustHandler = new UserRemoveTrustClaimEventHandler();
    private static final BorderClaimEventHandler borderClaimHandler = new BorderClaimEventHandler();

    public static void loadClaimsForcibly(ICommandSender sender){
        final AtomicInteger counter = new AtomicInteger(0);
        Tiquality.LOGGER.info("Importing griefprevention claims...");
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "[Tiquality] Import started."));

        List<Claim> list = new ArrayList<>();
        for (World world : Sponge.getServer().getWorlds()) {
            list.addAll(GriefPrevention.getApi().getClaimManager(world).getWorldClaims());
        }
        for(Claim claim : list){
            counter.getAndIncrement();
            Text owner = claim.getOwnerName();
            net.minecraft.world.World world = (net.minecraft.world.World) claim.getWorld();
            Location<World> pos = claim.getLesserBoundaryCorner();

            String identifier =
                    (owner != null ? owner.toPlain() : "Unknown") + " at DIM=" +
                            (world != null ? String.valueOf(world.provider.getDimension()) : "Unknown") + " "
                            + (pos != null ? "X: " + pos.getBlockX() + " Z: " + pos.getBlockZ() : "unknown");

            findOrGetTrackerByClaim(claim).setBlockTrackers(new Runnable() {
                @Override
                public void run() {
                    String message = "[Tiquality] Remaining: " + counter.getAndDecrement() + ". Imported: " + identifier;
                    Tiquality.LOGGER.info(message);
                    sender.sendMessage(new TextComponentString(TextFormatting.GREEN + message));
                    synchronized (counter) {
                        counter.notifyAll();
                    }
                }
            });
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (counter) {
                        while(counter.get() > 0){
                            counter.wait(5000);
                            int tasks = WorldHelper.getQueuedTasks();
                            sender.sendMessage(new TextComponentString(TextFormatting.DARK_GRAY + "[Tiquality] " + tasks + " tasks to process left."));
                        }
                    }
                    sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "[Tiquality] Import finished."));
                    Tiquality.LOGGER.info("Import finished.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static GriefPreventionTracker findOrGetTrackerByClaim(@Nonnull Claim claim){

        GriefPreventionTracker tracker = TrackerManager.foreach(new TrackerManager.Action<GriefPreventionTracker>() {
            @Override
            public void each(TrackerBase tracker) {
                if(tracker instanceof GriefPreventionTracker){
                    GriefPreventionTracker gpTracker = (GriefPreventionTracker) tracker;
                    if(gpTracker.doesClaimExists()){
                        if(gpTracker.claim.getUniqueId().equals(claim.getUniqueId())){
                            stop(gpTracker);
                        }
                    }
                }
            }
        });

        if(tracker != null){
            return tracker;
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
        Sponge.getEventManager().registerListener(Tiquality.INSTANCE, BorderClaimEvent.class, borderClaimHandler);

        Tracking.registerCustomTracker(new GriefPreventionTracker(null));
        MinecraftForge.EVENT_BUS.register(EventHandler.INSTANCE);
    }


    private static class CreateClaimEventHandler implements EventListener<CreateClaimEvent>{
        @Override
        public void handle(@Nonnull CreateClaimEvent event) {
            /*
                Using a workaround since the claims are not fully populated yet.
             */
            Tiquality.SCHEDULER.schedule(new Runnable() {
                @Override
                public void run() {
                    for(Claim claim : event.getClaims()){
                        MinecraftForge.EVENT_BUS.post(new GPClaimCreatedFullyEvent(claim));
                    }
                }
            });
        }
    }

    private static class ChangeClaimEventHandler implements EventListener<ChangeClaimEvent>{
        @Override
        public void handle(@Nonnull ChangeClaimEvent event) {
            for(Claim claim : event.getClaims()){
                GriefPreventionTracker tracker = findOrGetTrackerByClaim(claim);
                tracker.setBlockTrackers(null);
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
                player.sendMessage(new TextComponentString(TextFormatting.DARK_GRAY + "[Tiquality] Claim removal detected, updating trackers..."));

                for(Claim claim : event.getClaims()){
                    GriefPreventionTracker tracker = findOrGetTrackerByClaim(claim);
                    tracker.replaceTracker(PlayerTracker.getOrCreatePlayerTrackerByProfile(player.getGameProfile()));
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
            if(claim.isWilderness() && entity.getTracker() instanceof GriefPreventionTracker){
                entity.setTracker(null);
            }else{
                entity.setTracker(findOrGetTrackerByClaim(claim));
            }
        }
    }
}
