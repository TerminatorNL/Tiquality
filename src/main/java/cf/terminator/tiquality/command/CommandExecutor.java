package cf.terminator.tiquality.command;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.api.TiqualityException;
import cf.terminator.tiquality.integration.ExternalHooker;
import cf.terminator.tiquality.integration.griefprevention.GriefPreventionHook;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.interfaces.UpdateTyped;
import cf.terminator.tiquality.monitor.InfoMonitor;
import cf.terminator.tiquality.monitor.TrackingTool;
import cf.terminator.tiquality.profiling.AnalyzedComponent;
import cf.terminator.tiquality.profiling.ProfileReport;
import cf.terminator.tiquality.profiling.SimpleProfiler;
import cf.terminator.tiquality.profiling.TickTime;
import cf.terminator.tiquality.tracking.PlayerTracker;
import cf.terminator.tiquality.tracking.TrackerManager;
import cf.terminator.tiquality.tracking.UpdateType;
import cf.terminator.tiquality.util.ForgeData;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;

import static cf.terminator.tiquality.Tiquality.PREFIX;
import static cf.terminator.tiquality.Tiquality.SCHEDULER;
import static cf.terminator.tiquality.TiqualityConfig.MAX_CLAIM_RADIUS;
import static cf.terminator.tiquality.util.Utils.ONE_DECIMAL_FORMATTER;
import static cf.terminator.tiquality.util.Utils.TWO_DECIMAL_FORMATTER;

@SuppressWarnings({"NoTranslation", "WeakerAccess"})
public class CommandExecutor {

    public static void incorrectUsageError(ICommandSender sender, PermissionHolder holder) throws CommandException {
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Running Tiquality version: " + TextFormatting.AQUA + Tiquality.VERSION));
        sender.sendMessage(new TextComponentString(""));
        sender.sendMessage(new TextComponentString(getUsage(holder)));
        throw new CommandException("Hint: Press TAB for suggestions.");
    }

    public static String getUsage(PermissionHolder holder){
        StringBuilder builder = new StringBuilder();

        if (holder.hasPermission(PermissionHolder.Permission.USE)) {
            builder.append("Usage: /tiquality <info [point] | track | share | notify | profile <secs>");
        }
        if (holder.hasPermission(PermissionHolder.Permission.ADMIN)) {
            builder.append(" [target] | reload | set");
        }
        if (holder.hasPermission(PermissionHolder.Permission.CLAIM)) {
            builder.append(" | claim");
        }
        return builder.toString();
    }

    public static void execute(ICommandSender sender, String[] args, PermissionHolder holder) throws CommandException{
        holder.checkPermission(PermissionHolder.Permission.USE);
        if(args.length == 0){
            incorrectUsageError(sender, holder);
        }
        /*

                RELOAD

         */
        if(args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
            holder.checkPermission(PermissionHolder.Permission.ADMIN);
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Reloading..."));
            TiqualityConfig.QuickConfig.reloadFromFile();
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Done!"));
        /*

                SHARE

         */
        }else if(args[0].equalsIgnoreCase("share")) {
            if (sender instanceof EntityPlayer == false) {
                throw new CommandException("Only players can use the 'share' command.");
            }
            holder.checkPermission(PermissionHolder.Permission.USE);
            PlayerTracker tracker = PlayerTracker.getOrCreatePlayerTrackerByProfile((TiqualityWorld) sender.getEntityWorld(), ((EntityPlayer) sender).getGameProfile());

            if(args.length != 2){
                List<TextComponentString> list = tracker.getSharedToTextual((TiqualityWorld) sender.getEntityWorld());
                if(list.size() > 0) {
                    sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "You are currently sharing your tick time with: "));
                    for (TextComponentString t : list) {
                        sender.sendMessage(t);
                    }
                }
                throw new CommandException("Usage: /tiquality share [name]");
            }
            String name = args[1];
            GameProfile targetPlayer = ForgeData.getGameProfileByName(name);
            if(targetPlayer == null){
                throw new CommandException("Sorry, the user '" + name + "' was not found.");
            }
            PlayerTracker targetTracker = PlayerTracker.getOrCreatePlayerTrackerByProfile((TiqualityWorld) sender.getEntityWorld(), targetPlayer);

            boolean newState = tracker.switchSharedTo(targetTracker.getHolder().getId());

            if(newState == true) {
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "You are now sharing your tick time with: " + targetPlayer.getName()));
                sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "If you want to stop sharing your time, run this command again!"));
            }else{
                sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "You are no longer sharing your tick time with: " + targetPlayer.getName()));
                sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "If you want to share your time again, run this command again!"));
            }
        /*

                INFO

         */
        }else if(args[0].equalsIgnoreCase("info")) {
            if (sender instanceof EntityPlayer == false) {
                throw new CommandException("Only players can use the 'info' command.");
            }
            holder.checkPermission(PermissionHolder.Permission.USE);
            EntityPlayer player = (EntityPlayer) sender;
            if(args.length > 1 && args[1].equalsIgnoreCase("point")){
                new InfoMonitor((EntityPlayerMP) player).start(5000);
                return;
            }

            World world = player.getEntityWorld();

            BlockPos blockPosAtFeet = player.getPosition();
            BlockPos blockPosBelowFeet = player.getPosition().down();

            IBlockState stateAtFeet = player.getEntityWorld().getBlockState(blockPosAtFeet);
            IBlockState stateBelowFeet = player.getEntityWorld().getBlockState(blockPosBelowFeet);

            Block blockAtFeet = stateAtFeet.getBlock();
            Block blockBelowFeet = stateBelowFeet.getBlock();

            UpdateType feetUpdateType = ((UpdateTyped) blockAtFeet).getUpdateType();
            UpdateType belowUpdateType = ((UpdateTyped) blockBelowFeet).getUpdateType();

            boolean isBlockAtFeetAir = blockAtFeet.isAir(stateAtFeet, world, blockPosAtFeet);
            boolean isBlockBelowFeetAir = blockBelowFeet.isAir(stateBelowFeet, world, blockPosBelowFeet);

            player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Info:"));
            if (isBlockAtFeetAir && isBlockBelowFeetAir) {
                throw new CommandException("Please stand on top of a block and run this command again.");
            }
            if (isBlockBelowFeetAir == false) {
                Tracker tracker = ((TiqualityWorld) player.getEntityWorld()).getTiqualityTracker(player.getPosition().down());
                TextComponentString message = tracker == null ? new TextComponentString(TextFormatting.AQUA + "Not tracked") : tracker.getInfo();
                player.sendMessage(new TextComponentString(TextFormatting.WHITE + "Block below: " +
                        TextFormatting.YELLOW + Block.REGISTRY.getNameForObject(blockBelowFeet).toString() + TextFormatting.WHITE + " TickType: ").appendSibling(belowUpdateType.getText()).appendSibling(new TextComponentString(TextFormatting.WHITE + " Status: " + message.getText())));
            }
            if (isBlockAtFeetAir == false) {
                Tracker tracker = ((TiqualityWorld) player.getEntityWorld()).getTiqualityTracker(player.getPosition());
                TextComponentString message = tracker == null ? new TextComponentString(TextFormatting.AQUA + "Not tracked") : tracker.getInfo();
                player.sendMessage(new TextComponentString(TextFormatting.WHITE + "Block at feet: " +
                        TextFormatting.YELLOW + Block.REGISTRY.getNameForObject(blockAtFeet).toString() + TextFormatting.WHITE + " TickType: ").appendSibling(feetUpdateType.getText()).appendSibling(new TextComponentString(TextFormatting.WHITE + " Status: " + message.getText())));
            }
        /*

                TRACK

         */
        }else if(args[0].equalsIgnoreCase("track")){
            if(sender instanceof EntityPlayerMP == false){
                throw new CommandException("Only players can use this command!");
            }
            holder.checkPermission(PermissionHolder.Permission.USE);
            int time = 5;
            if(args.length >= 2){
                time = CommandBase.parseInt(args[1],0,60);
            }

            new TrackingTool((EntityPlayerMP) sender).start(time * 1000);
        /*

                SET

         */
        }else if(args[0].equalsIgnoreCase("set")) {
            holder.checkPermission(PermissionHolder.Permission.ADMIN);
            if (sender instanceof EntityPlayer == false) {
                throw new CommandException("Only players can use the 'set' command.");
            }
            EntityPlayer player = (EntityPlayer) sender;
            if (args.length != 3) {
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: set <feet|below> ").appendSibling(UpdateType.getArguments(TextFormatting.RED)));
                throw new CommandException("Hover over the different update types to see more info!");
            }
            String mode = args[1];
            Block blockToAdd;
            if (mode.equalsIgnoreCase("feet")) {
                blockToAdd = player.getEntityWorld().getBlockState(player.getPosition()).getBlock();
                if (player.getEntityWorld().isAirBlock(player.getPosition())) {
                    throw new CommandException("Please stand with your feet in a block (like water or a flower) and run this command again.");
                }
            } else if (mode.equalsIgnoreCase("below")) {
                blockToAdd = player.getEntityWorld().getBlockState(player.getPosition().down()).getBlock();
                if (player.getEntityWorld().isAirBlock(player.getPosition().down())) {
                    throw new CommandException("Please stand on top of a block and run this command again.");
                }
            } else {
                sender.sendMessage(new TextComponentString("Invalid input: '" + mode + "'. Expected 'feet' or 'below'"));
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: set <feet|below> ").appendSibling(UpdateType.getArguments(TextFormatting.RED)));
                throw new CommandException("Hover over the different update types to see more info!");
            }

            String type = args[2].toUpperCase();
            UpdateType updateType;
            try{
                updateType = UpdateType.valueOf(type);
            }catch (IllegalArgumentException e){
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Invalid update type! Valid types: ").appendSibling(UpdateType.getArguments(TextFormatting.RED)));
                throw new CommandException("Hover over the different update types to see more info!");
            }

            SCHEDULER.schedule(new Runnable() {
                @Override
                public void run() {
                    ResourceLocation resourceLocation = Block.REGISTRY.getNameForObject(blockToAdd);
                    String identifier = resourceLocation.getNamespace() + ":" + resourceLocation.getPath();

                    /* Clear current status */
                    {
                        ArrayList<String> list = new ArrayList<>(Arrays.asList(TiqualityConfig.BLOCK_TICK_BEHAVIOR.NATURAL_BLOCKS));
                        list.remove(identifier);
                        TiqualityConfig.BLOCK_TICK_BEHAVIOR.NATURAL_BLOCKS = list.toArray(new String[0]);
                    }
                    {
                        ArrayList<String> list = new ArrayList<>(Arrays.asList(TiqualityConfig.BLOCK_TICK_BEHAVIOR.ALWAYS_TICKED_BLOCKS));
                        list.remove(identifier);
                        TiqualityConfig.BLOCK_TICK_BEHAVIOR.ALWAYS_TICKED_BLOCKS = list.toArray(new String[0]);

                    }
                    {
                        ArrayList<String> list = new ArrayList<>(Arrays.asList(TiqualityConfig.BLOCK_TICK_BEHAVIOR.PRIORITY_BLOCKS));
                        list.remove(identifier);
                        TiqualityConfig.BLOCK_TICK_BEHAVIOR.PRIORITY_BLOCKS = list.toArray(new String[0]);

                    }
                    {
                        ArrayList<String> list = new ArrayList<>(Arrays.asList(TiqualityConfig.BLOCK_TICK_BEHAVIOR.TICK_DENIED_BLOCKS));
                        list.remove(identifier);
                        TiqualityConfig.BLOCK_TICK_BEHAVIOR.TICK_DENIED_BLOCKS = list.toArray(new String[0]);

                    }
                    /* Recalculate inheritance */
                    if(updateType == UpdateType.NATURAL) {
                        ArrayList<String> list = new ArrayList<>(Arrays.asList(TiqualityConfig.BLOCK_TICK_BEHAVIOR.NATURAL_BLOCKS));
                        list.add(identifier);
                        TiqualityConfig.BLOCK_TICK_BEHAVIOR.NATURAL_BLOCKS = list.toArray(new String[0]);
                    }else if(updateType == UpdateType.ALWAYS_TICK){
                        ArrayList<String> list = new ArrayList<>(Arrays.asList(TiqualityConfig.BLOCK_TICK_BEHAVIOR.ALWAYS_TICKED_BLOCKS));
                        list.add(identifier);
                        TiqualityConfig.BLOCK_TICK_BEHAVIOR.ALWAYS_TICKED_BLOCKS = list.toArray(new String[0]);
                    }else if(updateType == UpdateType.PRIORITY){
                        ArrayList<String> list = new ArrayList<>(Arrays.asList(TiqualityConfig.BLOCK_TICK_BEHAVIOR.PRIORITY_BLOCKS));
                        list.add(identifier);
                        TiqualityConfig.BLOCK_TICK_BEHAVIOR.PRIORITY_BLOCKS = list.toArray(new String[0]);
                    }else if(updateType == UpdateType.TICK_DENIED){
                        ArrayList<String> list = new ArrayList<>(Arrays.asList(TiqualityConfig.BLOCK_TICK_BEHAVIOR.TICK_DENIED_BLOCKS));
                        list.add(identifier);
                        TiqualityConfig.BLOCK_TICK_BEHAVIOR.TICK_DENIED_BLOCKS = list.toArray(new String[0]);
                    }
                    TiqualityConfig.QuickConfig.saveToFile();
                    TiqualityConfig.QuickConfig.update();
                    sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Updated: " + TextFormatting.YELLOW + identifier + TextFormatting.GREEN + ". New tick type: " + updateType.getText().getFormattedText()));
                }
            });
        /*

            CLAIM

         */
        }else if(args[0].equalsIgnoreCase("claim")){
            holder.checkPermission(PermissionHolder.Permission.CLAIM);
            int range = MAX_CLAIM_RADIUS;
            if(args.length > 1){
                range = CommandBase.parseInt(args[1],1, MAX_CLAIM_RADIUS);
            }
            if(sender instanceof EntityPlayer == false){
                throw new CommandException("You must be a player to use this command.");
            }
            EntityPlayer player = (EntityPlayer) sender;
            BlockPos leastPos = player.getPosition().add(range * -1, 0, range * -1);
            BlockPos mostPos = player.getPosition().add(range, 0, range);

            leastPos = new BlockPos(leastPos.getX(), 0, leastPos.getZ());
            mostPos = new BlockPos(mostPos.getX(), 255, mostPos.getZ());

            player.sendMessage(new TextComponentString(PREFIX + "Claiming area: x=" + leastPos.getX() + " z=" + leastPos.getZ() + " to x=" + mostPos.getX() + " z=" + mostPos.getZ()));
            Tracker tracker = PlayerTracker.getOrCreatePlayerTrackerByProfile((TiqualityWorld) player.world,player.getGameProfile());
            ((TiqualityWorld) player.getEntityWorld()).setTiqualityTrackerCuboidAsync(leastPos, mostPos, tracker, new Runnable() {
                @Override
                public void run() {
                    player.sendMessage(new TextComponentString(PREFIX + "Done."));
                }
            });
        /*

            NOTIFY

         */
        }else if(args[0].equalsIgnoreCase("notify")){
            holder.checkPermission(PermissionHolder.Permission.USE);
            if(sender instanceof EntityPlayer == false){
                throw new CommandException("Only players can use the notify command!");
            }
            EntityPlayer player = (EntityPlayer) sender;
            boolean newNotifySetting = TrackerManager.foreach(new TrackerManager.Action<PlayerTracker>() {
                @Override
                public void each(Tracker tracker) {
                    if(tracker instanceof PlayerTracker){
                        if(((PlayerTracker) tracker).getOwner().getId().equals(player.getGameProfile().getId())){
                            stop((PlayerTracker) tracker);
                        }
                    }
                }
            }).switchNotify();

            sender.sendMessage(new TextComponentString(PREFIX + "Notifications " + (newNotifySetting ? "enabled" : "disabled") + "."));
        /*

            PROFILE

         */
        }else if(args[0].equalsIgnoreCase("profile")){
            holder.checkPermission(PermissionHolder.Permission.USE);
            if(args.length == 1) {
                incorrectUsageError(sender, holder);
            }
            int time = CommandBase.parseInt(args[1],1,30);
            GameProfile target_player;
            if(args.length == 3){
                String target = args[2];
                holder.checkPermission(PermissionHolder.Permission.ADMIN);
                target_player = ForgeData.getGameProfileByName(target);
            }else{
                if (sender instanceof EntityPlayer == false) {
                    throw new CommandException("You need to specify a playername or UUID.");
                }
                target_player = (((EntityPlayer) sender).getGameProfile());
            }
            if(target_player == null){
                throw new CommandException("Player not found.");
            }
            LinkedList<Tracker> trackersToProfile = new LinkedList<>();


            TrackerManager.foreach(new TrackerManager.Action<Object>() {
                @Override
                public void each(Tracker tracker) {
                    if(tracker.canProfile() && tracker.getAssociatedPlayers().contains(target_player)){
                        trackersToProfile.add(tracker);
                    }
                }
            });

            if(trackersToProfile.size() == 0){
                throw new CommandException("Player is found, but there are no trackers associated.");
            }
            sender.sendMessage(new TextComponentString(PREFIX + "Profiler started"));

            for(Tracker tracker : trackersToProfile) {
                SimpleProfiler profiler = new SimpleProfiler(tracker, time * 1000, new SimpleProfiler.ProfilePrinter() {
                    @Override
                    public void progressUpdate(ITextComponent message) {
                        sender.sendMessage(message);
                    }

                    @Override
                    public void report(ProfileReport report) {

                        LinkedList<ITextComponent> messages = new LinkedList<>();

                        Iterator<AnalyzedComponent> componentIterator = report.getAnalyzedComponents().descendingIterator();
                        for(int i=0;i<50 && componentIterator.hasNext(); i++){
                            AnalyzedComponent component = componentIterator.next();

                            long muPerTick = Math.round(report.getMuPerTick(component.getTimes()));
                            double callsPerTick = report.getCallsPerTick(component.getTimes());

                            String stats = TextFormatting.DARK_AQUA + report.getTrackerImpactPercentage(component.getTimes()) + TextFormatting.GRAY + "% "
                                    + TextFormatting.WHITE + muPerTick + TextFormatting.GRAY + "µs/t "
                                    + TextFormatting.WHITE + ONE_DECIMAL_FORMATTER.format(callsPerTick) + TextFormatting.GRAY + "c/t "
                                    + component.getLocationString() + TextFormatting.GRAY + " ";

                            StringBuilder name = new StringBuilder(component.getFriendlyName());


                            int upperBound = name.length();
                            int freeCharacters = 85 - stats.length();
                            int suggestedLength = Math.min(upperBound, Math.max(3,freeCharacters));

                            boolean ellipsis = false;
                            if(suggestedLength < name.length()){
                                ellipsis = true;
                            }

                            name = new StringBuilder(name.substring(0, suggestedLength));
                            if(ellipsis){
                                name.append("...");
                            }

                            /* On hover */
                            Style style = new Style();
                            ResourceLocation location = component.getResourceLocation();

                            TextComponentString hoverString = new TextComponentString(PREFIX + "Details"
                                    + TextFormatting.GRAY + "\nName: " + TextFormatting.WHITE + component.getFriendlyName()
                                    + TextFormatting.GRAY + "\nResource: " + TextFormatting.WHITE + (location == null ? "Not available" : location.toString())
                                    + TextFormatting.GRAY + "\nClass: " + TextFormatting.WHITE + component.getReferencedClass()
                                    + "\n"
                                    + TextFormatting.GRAY + "\nImpact on server: " + TextFormatting.WHITE + report.getServerImpactPercentage(component.getTimes()) + "% " + TextFormatting.DARK_GRAY + "(Approx.)"
                                    + TextFormatting.GRAY + "\nImpact on tracker: " + TextFormatting.WHITE + report.getTrackerImpactPercentage(component.getTimes()) + "%"
                                    + "\n"
                                    + TextFormatting.GRAY + "\nµs per tick: " + TextFormatting.WHITE + muPerTick + TextFormatting.DARK_GRAY + " (Approx.)"
                                    + TextFormatting.GRAY + "\nCalls per tick: " + TextFormatting.WHITE + TWO_DECIMAL_FORMATTER.format(callsPerTick)
                                    + "\n"
                                    + TextFormatting.GRAY + "\nServer TPS: " + TextFormatting.WHITE + TWO_DECIMAL_FORMATTER.format(report.getServerTPS())
                                    + TextFormatting.GRAY + "\nTracker TPS: " + TextFormatting.WHITE + TWO_DECIMAL_FORMATTER.format(report.getTrackerTPS())
                                    + "\n"
                                    + TextFormatting.GRAY + "\nServer ticks measured: " + TextFormatting.WHITE + TWO_DECIMAL_FORMATTER.format(report.getServerTicks())
                                    + TextFormatting.GRAY + "\nTracker ticks measured: " + TextFormatting.WHITE + TWO_DECIMAL_FORMATTER.format(report.getTrackerTicks())
                            );

                            style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverString));




                            messages.add(new TextComponentString(stats + name).setStyle(style));
                        }

                        messages.add(new TextComponentString(PREFIX + TextFormatting.GREEN + TextFormatting.BOLD + "By object"));

                        Iterator<Map.Entry<String, TickTime>> classIterator = report.getClassTimesSorted().descendingIterator();
                        for(int i=0;i<50 && classIterator.hasNext(); i++){
                            Map.Entry<String, TickTime> entry = classIterator.next();
                            messages.add(new TextComponentString(
                                    TextFormatting.DARK_AQUA + report.getTrackerImpactPercentage(entry.getValue()) + TextFormatting.GRAY + "% "
                                        + TextFormatting.GRAY + entry.getKey()));
                        }
                        messages.add(new TextComponentString(PREFIX + TextFormatting.GREEN + TextFormatting.BOLD + "By class"));
                        messages.add(new TextComponentString(PREFIX + TextFormatting.GREEN + TextFormatting.BOLD + "REPORT"));

                        messages.descendingIterator().forEachRemaining(new Consumer<ITextComponent>() {
                            @Override
                            public void accept(ITextComponent iTextComponent) {
                                sender.sendMessage(iTextComponent);
                            }
                        });
                        sender.sendMessage(new TextComponentString(PREFIX + "Hover over the text for more details!"));




                    }
                });
                try {
                    profiler.start();
                } catch (TiqualityException e) {
                    e.printStackTrace();
                    sender.sendMessage(e.getTextComponent());
                }
            }

        /*

            DEBUG

         */
        }else if(args[0].equalsIgnoreCase("debug")){
            holder.checkPermission(PermissionHolder.Permission.ADMIN);
            Set<Tracker> set = new HashSet<>();
            TrackerManager.foreach(new TrackerManager.Action<Object>() {
                @Override
                public void each(Tracker tracker) {
                    set.add(tracker);
                }
            });

            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Loaded tracker objects (" + set.size() + "):"));
            for(Tracker e : set){
                sender.sendMessage(new TextComponentString(TextFormatting.AQUA + e.toString()));
            }

            if(sender instanceof Entity){
                Entity entity = (Entity) sender;

                /* FEET */
                BlockPos feet = entity.getPosition();
                String blockNameFeet = entity.getEntityWorld().getBlockState(feet).getBlock().toString();
                boolean isMarkedFeet = ((TiqualityWorld) entity.getEntityWorld()).tiquality_isMarkedThorough(feet);
                Tracker trackerFeet = ((TiqualityWorld) entity.getEntityWorld()).getTiqualityTracker(feet);
                String trackerTextFeet;
                if(trackerFeet == null){
                    trackerTextFeet = TextFormatting.AQUA + "NOT TRACKED";
                }else{
                    trackerTextFeet = TextFormatting.WHITE + trackerFeet.getInfo().getText();
                }
                if(isMarkedFeet){
                    sender.sendMessage(new TextComponentString("FEET: " + blockNameFeet + TextFormatting.RED + " MARKED " + trackerTextFeet));
                }else{
                    sender.sendMessage(new TextComponentString("FEET: " + blockNameFeet + TextFormatting.GREEN + " NOT MARKED " + trackerTextFeet));
                }

                /* BELOW */
                BlockPos below = entity.getPosition().down();
                String blockNameBelow = entity.getEntityWorld().getBlockState(below).getBlock().toString();
                boolean isMarkedBelow = ((TiqualityWorld) entity.getEntityWorld()).tiquality_isMarkedThorough(below);
                Tracker trackerBelow = ((TiqualityWorld) entity.getEntityWorld()).getTiqualityTracker(below);
                String trackerTextBelow;
                if(trackerBelow == null){
                    trackerTextBelow = TextFormatting.AQUA + "NOT TRACKED";
                }else{
                    trackerTextBelow = TextFormatting.WHITE + trackerBelow.getInfo().getText();
                }
                if(isMarkedBelow){
                    sender.sendMessage(new TextComponentString("BELOW: " + blockNameBelow + TextFormatting.RED + " MARKED " + trackerTextBelow));
                }else{
                    sender.sendMessage(new TextComponentString("BELOW: " + blockNameBelow + TextFormatting.GREEN + " NOT MARKED " + trackerTextBelow));
                }
            }


        /*

                GRIEFPREVENTION IMPORT

         */
        }else if(args[0].equalsIgnoreCase("import_griefprevention")){
            holder.checkPermission(PermissionHolder.Permission.ADMIN);
            if(ExternalHooker.LOADED_HOOKS.contains("griefprevention")){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        GriefPreventionHook.loadClaimsForcibly(sender);
                    }
                }, "Tiquality GP import thread").start();
            }else{
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "GriefPrevention not detected."));
            }
        }else if(args[0].equalsIgnoreCase("import_griefprevention_claim")){
            holder.checkPermission(PermissionHolder.Permission.USE);
            if(sender instanceof EntityPlayer == false){
                throw new CommandException("Only players can use this command.");
            }
            if(ExternalHooker.LOADED_HOOKS.contains("griefprevention")){
                GriefPreventionHook.importSingleClaim((EntityPlayer) sender);
            }else{
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "GriefPrevention not detected."));
            }
        /*

                UNKNOWN ARGUMENT

         */
        }else{
            incorrectUsageError(sender, holder);
        }
    }

    public static List<String> getSuggestions(@Nonnull String[] args, @Nonnull PermissionHolder holder){
        ArrayList<String> list = new ArrayList<>();
        if(args.length == 1){
            String start = args[0];
            addIfStartsWith(list, start, "track");
            addIfStartsWith(list, start, "info");
            addIfStartsWith(list, start, "profile");
            addIfStartsWith(list, start, "notify");
            addIfStartsWith(list, start, "share");
            if(holder.hasPermission(PermissionHolder.Permission.ADMIN)){
                addIfStartsWith(list, start, "set");
                addIfStartsWith(list, start, "reload");
            }
            if (holder.hasPermission(PermissionHolder.Permission.CLAIM)) {
                addIfStartsWith(list, start, "claim");
            }
            return list;
        }else if(args.length == 2){
            String start = args[1];
            if(args[0].equalsIgnoreCase("info")){
                addIfStartsWith(list, start, "point");
            }else if(args[0].equalsIgnoreCase("set")){
                addIfStartsWith(list, start, "below");
                addIfStartsWith(list, start, "feet");
            }else if(args[0].equalsIgnoreCase("track")){
                addIfStartsWith(list, start, "1");
                addIfStartsWith(list, start, "5");
                addIfStartsWith(list, start, "10");
                addIfStartsWith(list, start, "20");
            }else if(args[0].equalsIgnoreCase("profile")){
                addIfStartsWith(list, start, "5");
            }else if(args[0].equalsIgnoreCase("claim")){
                if (holder.hasPermission(PermissionHolder.Permission.CLAIM)) {
                    addIfStartsWith(list, start, String.valueOf(MAX_CLAIM_RADIUS));
                }
            }
        }else if (args.length == 3){
            String start = args[2];
            if(args[0].equalsIgnoreCase("profile")) {
                if(holder.hasPermission(PermissionHolder.Permission.ADMIN)) {
                    Set<GameProfile> profiles = new HashSet<>();
                    TrackerManager.foreach(new TrackerManager.Action<Object>() {
                        @Override
                        public void each(Tracker tracker) {
                            profiles.addAll(tracker.getAssociatedPlayers());
                        }
                    });

                    for(GameProfile profile : profiles){
                        addIfStartsWith(list, start, profile.getName());
                    }
                }
            }else if(args[0].equalsIgnoreCase("set")){
                for(UpdateType updateType : UpdateType.values()){
                    addIfStartsWith(list, start, updateType.name());
                }
            }
        }
        return list;
    }

    public static void addIfStartsWith(ArrayList<String> list, String starts, String target){
        if(target.toLowerCase().startsWith(starts.toLowerCase())){
            list.add(target);
        }
    }
}
