package cf.terminator.tiquality.integration.griefprevention;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.Tracking;
import cf.terminator.tiquality.integration.griefprevention.event.GPClaimCreatedFullyEvent;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.tracking.PlayerTracker;
import cf.terminator.tiquality.util.ForgeData;
import cf.terminator.tiquality.util.Scheduler;
import cf.terminator.tiquality.world.WorldHelper;
import com.mojang.authlib.GameProfile;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.api.event.ChangeClaimEvent;
import me.ryanhamshire.griefprevention.api.event.CreateClaimEvent;
import me.ryanhamshire.griefprevention.api.event.TransferClaimEvent;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("NoTranslation")
public class GriefPreventionHook {

    private static final CreateClaimEventHandler createClaimHandler = new CreateClaimEventHandler();
    private static final ChangeClaimEventHandler claimChangeHandler = new ChangeClaimEventHandler();
    private static final TransferClaimEventHandler transferClaimHandler = new TransferClaimEventHandler();
    //private static final BorderClaimEventHandler borderClaimHandler = new BorderClaimEventHandler();

    public static void setClaimTrackers(Claim claim, Tracker tracker, Runnable callback, Runnable beforeRun){
        Location<World> least = claim.getLesserBoundaryCorner();
        Location<World> most = claim.getGreaterBoundaryCorner();
        if(least == null || most == null){
            return;
        }
        TiqualityWorld world = (TiqualityWorld) least.getExtent();
        BlockPos leastPos = new BlockPos(least.getBlockX(), least.getBlockY(), least.getBlockZ());
        BlockPos mostPos = new BlockPos(most.getBlockX(), most.getBlockY(), most.getBlockZ());
        world.setTiqualityTrackerCuboidAsync(leastPos, mostPos, tracker,callback, beforeRun);
    }

    public static void loadClaimsForcibly(ICommandSender sender){
        final AtomicInteger counter = new AtomicInteger(0);
        Tiquality.LOGGER.info("Importing griefprevention claims...");
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "[Tiquality] Import started."));

        List<Claim> list = new ArrayList<>();
        for (World world : Sponge.getServer().getWorlds()) {
            list.addAll(GriefPrevention.getApi().getClaimManager(world).getWorldClaims());
        }
        for(Claim claim : list){
            if(GriefPreventionHook.isValidClaim(claim) == false){
                continue;
            }
            counter.getAndIncrement();
            Text owner = claim.getOwnerName();
            net.minecraft.world.World world = (net.minecraft.world.World) claim.getWorld();
            Location<World> pos = claim.getLesserBoundaryCorner();

            String identifier =
                    (owner != null ? owner.toPlain() : "Unknown") + " at DIM=" +
                            (world != null ? String.valueOf(world.provider.getDimension()) : "Unknown") + " "
                            + (pos != null ? "X: " + pos.getBlockX() + " Z: " + pos.getBlockZ() : "unknown");


            setClaimTrackers(claim, PlayerTracker.getOrCreatePlayerTrackerByProfile((TiqualityWorld) world, ForgeData.getGameProfileByUUID(claim.getOwnerUniqueId())), new Runnable() {
                @Override
                public void run() {
                    String message = "[Tiquality] Remaining claims: " + (counter.getAndDecrement() - 1);
                    Tiquality.LOGGER.info(message);
                    sender.sendMessage(new TextComponentString(TextFormatting.GREEN + message));
                    synchronized (counter) {
                        counter.notifyAll();
                    }
                }
            }, new Runnable() {
                @Override
                public void run() {
                    Tiquality.LOGGER.info("Importing: " + identifier);
                }
            });
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(counter.get() > 0){
                        synchronized (counter) {
                            counter.wait(5000);
                        }
                        int tasks = WorldHelper.getQueuedTasks();
                        sender.sendMessage(new TextComponentString(TextFormatting.DARK_GRAY + "[Tiquality] " + tasks + " tasks to process left."));
                    }
                    sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "[Tiquality] Import finished."));
                    Tiquality.LOGGER.info("Import finished.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static boolean isValidClaim(Claim claim){
        /*
            Output reversed to improve readability.
         */
        boolean isInvalid = claim == null || claim.isWilderness() || claim.getOwnerUniqueId() == null;
        return isInvalid == false;
    }

    @Nullable
    public static Tracker findOrGetTrackerByClaim(@Nonnull Claim claim){
        if(claim.isWilderness()){
            return null;
        }else if(claim.isAdminClaim()){
            return AdminClaimTracker.INSTANCE;
        }else if(claim.getOwnerUniqueId() == null){
            throw new IllegalArgumentException("Claim owner is null!");
        }
        GameProfile profile = ForgeData.getGameProfileByUUID(claim.getOwnerUniqueId());
        return PlayerTracker.getOrCreatePlayerTrackerByProfile((TiqualityWorld) claim.getWorld(), profile);
    }


    public static void importSingleClaim(EntityPlayer sender) throws CommandException {
        BlockPos pos = sender.getPosition();

        Claim claim = GriefPrevention.getApi().getClaimManager((World) sender.getEntityWorld()).getClaimAt(new Location<>((World) sender.getEntityWorld(),pos.getX(), pos.getY(), pos.getZ()));

        if(claim == null){
            throw new CommandException("Claim not found, please stand in your claim where no tracker is present and run the command again.");
        }

        if(GriefPreventionHook.isValidClaim(claim) == false){
            throw new CommandException("Claim is found, but it is not of a valid type.");
        }

        TiqualityWorld world = (TiqualityWorld) sender.getEntityWorld();

        Tracker existingTracker = world.getTiqualityTracker(pos);
        if(existingTracker != null){
            throw new CommandException("There's already a tracker present: " + existingTracker.getInfo().getText());
        }
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "[Tiquality] Import queued."));

        Location<World> least = claim.getLesserBoundaryCorner();
        Location<World> most = claim.getGreaterBoundaryCorner();

        if(least == null || most == null){
            return;
        }

        BlockPos leastPos = new BlockPos(least.getBlockX(), least.getBlockY(), least.getBlockZ());
        BlockPos mostPos = new BlockPos(most.getBlockX(), most.getBlockY(), most.getBlockZ());

        world.setTiqualityTrackerCuboidAsync(leastPos, mostPos, PlayerTracker.getOrCreatePlayerTrackerByProfile(world, ForgeData.getGameProfileByUUID(claim.getOwnerUniqueId())), new Runnable() {
            @Override
            public void run() {
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Importing your claim..."));
            }
        }, new Runnable() {
            @Override
            public void run() {
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Import complete!"));
            }
        });
    }

    public static void init(){
        Sponge.getEventManager().registerListener(Tiquality.INSTANCE, CreateClaimEvent.class, createClaimHandler);
        Sponge.getEventManager().registerListener(Tiquality.INSTANCE, ChangeClaimEvent.class, claimChangeHandler);
        Sponge.getEventManager().registerListener(Tiquality.INSTANCE, TransferClaimEvent.class, transferClaimHandler);
        //Sponge.getEventManager().registerListener(Tiquality.INSTANCE, BorderClaimEvent.class, borderClaimHandler);

        Tracking.registerCustomTracker("GPAdmin", AdminClaimTracker.class);
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
            if(event instanceof ChangeClaimEvent.Type){
                ChangeClaimEvent.Type typeChangeEvent = (ChangeClaimEvent.Type) event;
                for(Claim claim : event.getClaims()){
                    ClaimType originalType = typeChangeEvent.getOriginalType();
                    ClaimType newType = typeChangeEvent.getType();
                    if(originalType == ClaimType.BASIC || originalType == ClaimType.TOWN || originalType == ClaimType.SUBDIVISION){
                        if(newType == ClaimType.ADMIN){
                            setClaimTrackers(claim, AdminClaimTracker.INSTANCE, null, null);
                        }
                    }else if(originalType == ClaimType.ADMIN){
                        if(newType == ClaimType.BASIC || newType == ClaimType.TOWN || newType == ClaimType.SUBDIVISION) {
                            Location<World> lesser = claim.getLesserBoundaryCorner();
                            Scheduler.INSTANCE.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    TiqualityWorld world = (TiqualityWorld) claim.getWorld();

                                    Tracker newTracker = GriefPreventionHook.findOrGetTrackerByClaim(GriefPrevention.getApi().getClaimManager((World) world).getClaimAt(lesser));
                                    if(newTracker == null){
                                        return;
                                    }
                                    setClaimTrackers(claim, newTracker, null, null);
                                }
                            });

                        }
                    }
                }
            }else if(event instanceof ChangeClaimEvent.Resize){
                Claim resizedClaim = ((ChangeClaimEvent.Resize) event).getResizedClaim();
                Tracker tracker = GriefPreventionHook.findOrGetTrackerByClaim(resizedClaim);
                if(tracker == null){
                    return;
                }
                Tiquality.SCHEDULER.schedule(new Runnable() {
                    @Override
                    public void run() {
                        setClaimTrackers(resizedClaim, tracker, null,null);
                    }
                });
            }
        }
    }

    private static class TransferClaimEventHandler implements EventListener<TransferClaimEvent>{
        @Override
        public void handle(@Nonnull TransferClaimEvent event) {
            for(Claim claim : event.getClaims()){
                Tracker tracker = GriefPreventionHook.findOrGetTrackerByClaim(claim);
                if(tracker == null){
                    continue;
                }
                setClaimTrackers(claim, tracker, null, null);
            }
        }
    }

    /*
    private static class BorderClaimEventHandler implements EventListener<BorderClaimEvent>{
        @Override
        public void handle(@Nonnull BorderClaimEvent event) {
            Tracker tracker = GriefPreventionHook.findOrGetTrackerByClaim(event.getEnterClaim());
            if(tracker != null){
                TiqualityEntity entity = (TiqualityEntity) event.getTargetEntity();
                entity.setTrackerHolder(tracker.getHolder());
            }
        }
    }*/
}
