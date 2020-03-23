package com.github.terminatornl.tiquality.integration.griefdefender;

import com.flowpowered.math.vector.Vector3i;
import com.github.terminatornl.tiquality.Tiquality;
import com.github.terminatornl.tiquality.api.Tracking;
import com.github.terminatornl.tiquality.integration.griefdefender.event.GDClaimCreatedFullyEvent;
import com.github.terminatornl.tiquality.interfaces.TiqualityWorld;
import com.github.terminatornl.tiquality.interfaces.Tracker;
import com.github.terminatornl.tiquality.tracking.PlayerTracker;
import com.github.terminatornl.tiquality.util.ForgeData;
import com.github.terminatornl.tiquality.util.Scheduler;
import com.github.terminatornl.tiquality.world.WorldHelper;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.event.ChangeClaimEvent;
import com.griefdefender.api.event.CreateClaimEvent;
import com.griefdefender.api.event.TransferClaimEvent;
import com.mojang.authlib.GameProfile;
import net.kyori.event.EventSubscriber;
import net.kyori.event.PostOrders;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("NoTranslation")
public class GriefDefenderHook {

    private static final CreateClaimEventHandler createClaimHandler = new CreateClaimEventHandler();
    private static final ChangeClaimEventHandler claimChangeHandler = new ChangeClaimEventHandler();
    private static final TransferClaimEventHandler transferClaimHandler = new TransferClaimEventHandler();
    //private static final BorderClaimEventHandler borderClaimHandler = new BorderClaimEventHandler();

    public static void setClaimTrackers(Claim claim, Tracker tracker, Runnable callback, Runnable beforeRun) {
        Vector3i least = claim.getLesserBoundaryCorner();
        Vector3i most = claim.getGreaterBoundaryCorner();
        resizeClaimTrackers(claim, least, most, tracker, callback, beforeRun);
    }

    public static void resizeClaimTrackers(Claim claim, Vector3i least, Vector3i most, Tracker tracker, Runnable callback, Runnable beforeRun) {
        if (least == null || most == null) {
            return;
        }
        final Optional<World> worldOpt = Sponge.getServer().getWorld(claim.getWorldUniqueId());
        //noinspection OptionalIsPresent
        if (worldOpt.isPresent() == false) {
            return;
        }
        TiqualityWorld world = (TiqualityWorld) worldOpt.get();
        BlockPos leastPos = new BlockPos(least.getX(), least.getY(), least.getZ());
        BlockPos mostPos = new BlockPos(most.getX(), most.getY(), most.getZ());
        world.setTiqualityTrackerCuboidAsync(leastPos, mostPos, tracker, callback, beforeRun);
    }

    public static void loadClaimsForcibly(ICommandSender sender) {
        final AtomicInteger counter = new AtomicInteger(0);
        Tiquality.LOGGER.info("Importing griefdefender claims...");
        sender.sendMessage(new TextComponentString(Tiquality.PREFIX + "Import started."));

        List<Claim> list = new ArrayList<>();
        for (World world : Sponge.getServer().getWorlds()) {
            list.addAll(GriefDefender.getCore().getClaimManager(world.getUniqueId()).getWorldClaims());
        }
        for (Claim claim : list) {
            if (GriefDefenderHook.isValidClaim(claim) == false) {
                continue;
            }
            counter.getAndIncrement();
            UUID owner = claim.getOwnerUniqueId();

            final Optional<World> worldOpt = Sponge.getServer().getWorld(claim.getWorldUniqueId());
            //noinspection OptionalIsPresent
            if(!worldOpt.isPresent()) {
                return;
            }

            net.minecraft.world.World world = (net.minecraft.world.World) worldOpt.get();
            Vector3i pos = claim.getLesserBoundaryCorner();

            String identifier =
                    (owner != null ? owner : "Unknown") + " at DIM=" +
                            (world != null ? String.valueOf(world.provider.getDimension()) : "Unknown") + " "
                            + (pos != null ? "X: " + pos.getX() + " Z: " + pos.getZ() : "unknown");


            setClaimTrackers(claim, PlayerTracker.getOrCreatePlayerTrackerByProfile((TiqualityWorld) world, ForgeData.getGameProfileByUUID(claim.getOwnerUniqueId())), new Runnable() {
                @Override
                public void run() {
                    String message = Tiquality.PREFIX + "Remaining claims: " + (counter.getAndDecrement() - 1);
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
                    while (counter.get() > 0) {
                        synchronized (counter) {
                            counter.wait(5000);
                        }
                        int tasks = WorldHelper.getQueuedTasks();
                        sender.sendMessage(new TextComponentString(Tiquality.PREFIX + tasks + " tasks to process left."));
                    }
                    sender.sendMessage(new TextComponentString(Tiquality.PREFIX + "Import finished."));
                    Tiquality.LOGGER.info("Import finished.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static boolean isValidClaim(Claim claim) {
        /*
            Output reversed to improve readability.
         */
        boolean isInvalid = claim == null || claim.isWilderness() || claim.getOwnerUniqueId() == null;
        return isInvalid == false;
    }

    @Nullable
    public static Tracker findOrGetTrackerByClaim(@Nonnull Claim claim) {
        if (claim.isWilderness()) {
            return null;
        } else if (claim.isAdminClaim()) {
            return AdminClaimTracker.INSTANCE;
        } else if (claim.getOwnerUniqueId() == null) {
            throw new IllegalArgumentException("Claim owner is null!");
        }
        GameProfile profile = ForgeData.getGameProfileByUUID(claim.getOwnerUniqueId());
        final Optional<World> worldOpt = Sponge.getServer().getWorld(claim.getWorldUniqueId());
        //noinspection OptionalIsPresent
        if(!worldOpt.isPresent()) {
            return null;
        }
        return PlayerTracker.getOrCreatePlayerTrackerByProfile((TiqualityWorld) worldOpt.get(), profile);
    }


    public static void importSingleClaim(EntityPlayer sender) throws CommandException {
        BlockPos pos = sender.getPosition();

        Claim claim = GriefDefender.getCore().getClaimManager(((World) sender.getEntityWorld()).getUniqueId()).getClaimAt(new Vector3i(pos.getX(), pos.getY(), pos.getZ()));

        if (claim == null) {
            throw new CommandException("Claim not found, please stand in your claim where no tracker is present and run the command again.");
        }

        if (GriefDefenderHook.isValidClaim(claim) == false) {
            throw new CommandException("Claim is found, but it is not of a valid type.");
        }

        TiqualityWorld world = (TiqualityWorld) sender.getEntityWorld();

        Tracker existingTracker = world.getTiqualityTracker(pos);
        if (existingTracker != null) {
            throw new CommandException("There's already a tracker present: " + existingTracker.getInfo().getText());
        }
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "[Tiquality] Import queued."));

        Vector3i least = claim.getLesserBoundaryCorner();
        Vector3i most = claim.getGreaterBoundaryCorner();

        if (least == null || most == null) {
            return;
        }

        BlockPos leastPos = new BlockPos(least.getX(), least.getY(), least.getZ());
        BlockPos mostPos = new BlockPos(most.getX(), most.getY(), most.getZ());

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

    public static void init() {
        GriefDefender.getEventManager().getBus().register(CreateClaimEvent.class, createClaimHandler);
        GriefDefender.getEventManager().getBus().register(ChangeClaimEvent.class, claimChangeHandler);
        GriefDefender.getEventManager().getBus().register(TransferClaimEvent.class, transferClaimHandler);
        //GriefDefender.getEventManager().getBus().register(BorderClaimEvent.class, borderClaimHandler);

        Tracking.registerCustomTracker("GDAdmin", AdminClaimTracker.class);
        MinecraftForge.EVENT_BUS.register(EventHandler.INSTANCE);
    }


    private static class CreateClaimEventHandler implements EventSubscriber<CreateClaimEvent> {
        @Override
        public void invoke(@Nonnull CreateClaimEvent event) {
            /*
                Using a workaround since the claims are not fully populated yet.
             */
            Tiquality.SCHEDULER.schedule(new Runnable() {
                @Override
                public void run() {
                    for (Claim claim : event.getClaims()) {
                        MinecraftForge.EVENT_BUS.post(new GDClaimCreatedFullyEvent(claim));
                    }
                }
            });
        }
    }

    private static class ChangeClaimEventHandler implements EventSubscriber<ChangeClaimEvent> {

        @Override
        public int postOrder() {
            return PostOrders.LAST;//Incase event is cancelled.
        }

        @Override
        public void invoke(@Nonnull ChangeClaimEvent event) {
            if(event.cancelled()) {
                return;
            }

            if (event instanceof ChangeClaimEvent.Type) {
                ChangeClaimEvent.Type typeChangeEvent = (ChangeClaimEvent.Type) event;
                for (Claim claim : event.getClaims()) {
                    ClaimType originalType = typeChangeEvent.getOriginalType();
                    ClaimType newType = typeChangeEvent.getType();
                    if (originalType == ClaimTypes.BASIC || originalType == ClaimTypes.TOWN || originalType == ClaimTypes.SUBDIVISION) {
                        if (newType == ClaimTypes.ADMIN) {
                            setClaimTrackers(claim, AdminClaimTracker.INSTANCE, null, null);
                        }
                    } else if (originalType == ClaimTypes.ADMIN) {
                        if (newType == ClaimTypes.BASIC || newType == ClaimTypes.TOWN || newType == ClaimTypes.SUBDIVISION) {
                            Vector3i lesser = claim.getLesserBoundaryCorner();
                            Scheduler.INSTANCE.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    final Optional<World> worldOpt = Sponge.getServer().getWorld(claim.getWorldUniqueId());
                                    //noinspection OptionalIsPresent
                                    if (worldOpt.isPresent() == false) {
                                        return;
                                    }

                                    TiqualityWorld world = (TiqualityWorld) worldOpt.get();

                                    Tracker newTracker = GriefDefenderHook.findOrGetTrackerByClaim(GriefDefender.getCore().getClaimManager(((World) world).getUniqueId()).getClaimAt(lesser));
                                    if (newTracker == null) {
                                        return;
                                    }
                                    setClaimTrackers(claim, newTracker, null, null);
                                }
                            });

                        }
                    }
                }
            } else if (event instanceof ChangeClaimEvent.Resize) {
                final ChangeClaimEvent.Resize claimEvent = (ChangeClaimEvent.Resize) event;
                Claim claim = claimEvent.getClaim();
                Tracker tracker = GriefDefenderHook.findOrGetTrackerByClaim(claim);
                if (tracker == null) {
                    return;
                }
                Tiquality.SCHEDULER.schedule(new Runnable() {
                    @Override
                    public void run() {
                        resizeClaimTrackers(claim, claim.getLesserBoundaryCorner(), claim.getGreaterBoundaryCorner(), tracker, null, null);//TODO verify this acts as expected
                    }
                });
            }
        }
    }

    private static class TransferClaimEventHandler implements EventSubscriber<TransferClaimEvent> {
        @Override
        public void invoke(@Nonnull TransferClaimEvent event) {
            for (Claim claim : event.getClaims()) {
                Tracker tracker = GriefDefenderHook.findOrGetTrackerByClaim(claim);
                if (tracker == null) {
                    continue;
                }
                setClaimTrackers(claim, tracker, null, null);
            }
        }
    }

    /*
    private static class BorderClaimEventHandler implements EventListener<BorderClaimEvent>{
        @Override
        public void invoke(@Nonnull BorderClaimEvent event) {
            Tracker tracker = GriefDefenderHook.findOrGetTrackerByClaim(event.getEnterClaim());
            if(tracker != null){
                TiqualityEntity entity = (TiqualityEntity) event.getTargetEntity();
                entity.setTrackerHolder(tracker.getHolder());
            }
        }
    }*/
}
