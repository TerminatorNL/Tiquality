package cf.terminator.tiquality.command;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.integration.ExternalHooker;
import cf.terminator.tiquality.integration.griefprevention.GriefPreventionHook;
import cf.terminator.tiquality.interfaces.TiqualityBlock;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.monitor.InfoMonitor;
import cf.terminator.tiquality.monitor.TrackingTool;
import cf.terminator.tiquality.tracking.PlayerTracker;
import cf.terminator.tiquality.tracking.TrackerManager;
import cf.terminator.tiquality.tracking.UpdateType;
import cf.terminator.tiquality.util.ForgeData;
import cf.terminator.tiquality.util.SimpleProfiler;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import java.util.*;

import static cf.terminator.tiquality.Tiquality.SCHEDULER;
import static cf.terminator.tiquality.TiqualityConfig.MAX_CLAIM_RADIUS;

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
            builder.append("Usage: /tiquality <info [point] | track | share | profile <secs>");
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

            Block blockAtFeet = player.getEntityWorld().getBlockState(player.getPosition()).getBlock();
            Block blockBelowFeet = player.getEntityWorld().getBlockState(player.getPosition().down()).getBlock();

            UpdateType feetUpdateType = ((TiqualityBlock) blockAtFeet).getUpdateType();
            UpdateType belowUpdateType = ((TiqualityBlock) blockBelowFeet).getUpdateType();

            player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Info:"));
            if (blockAtFeet == Blocks.AIR && blockBelowFeet == Blocks.AIR) {
                throw new CommandException("Please stand on top of a block and run this command again.");
            }
            if (blockBelowFeet != Blocks.AIR) {
                Tracker tracker = ((TiqualityWorld) player.getEntityWorld()).getTiqualityTracker(player.getPosition().down());
                TextComponentString message = tracker == null ? new TextComponentString(TextFormatting.AQUA + "Not tracked") : tracker.getInfo();
                player.sendMessage(new TextComponentString(TextFormatting.WHITE + "Block below: " +
                        TextFormatting.YELLOW + Block.REGISTRY.getNameForObject(blockBelowFeet).toString() + TextFormatting.WHITE + " TickType: " + belowUpdateType.getText().getFormattedText() + TextFormatting.WHITE + " Status: " + message.getText()));
            }
            if (blockAtFeet != Blocks.AIR) {
                Tracker tracker = ((TiqualityWorld) player.getEntityWorld()).getTiqualityTracker(player.getPosition());
                TextComponentString message = tracker == null ? new TextComponentString(TextFormatting.AQUA + "Not tracked") : tracker.getInfo();
                player.sendMessage(new TextComponentString(TextFormatting.WHITE + "Block at feet: " +
                        TextFormatting.YELLOW + Block.REGISTRY.getNameForObject(blockAtFeet).toString() + TextFormatting.WHITE + " TickType: " + feetUpdateType.getText().getFormattedText() + TextFormatting.WHITE + " Status: " + message.getText()));
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
                throw new CommandException("Usage: set <feet|below> <DEFAULT|NATURAL|ALWAYS_TICK>");
            }
            String mode = args[1];
            Block blockToAdd;
            if (mode.equalsIgnoreCase("feet")) {
                blockToAdd = player.getEntityWorld().getBlockState(player.getPosition()).getBlock();
                if (blockToAdd == Blocks.AIR) {
                    throw new CommandException("Please stand with your feet in a block (like water) and run this command again.");
                }
            } else if (mode.equalsIgnoreCase("below")) {
                blockToAdd = player.getEntityWorld().getBlockState(player.getPosition().down()).getBlock();
                if (blockToAdd == Blocks.AIR) {
                    throw new CommandException("Please stand on top of a block and run this command again.");
                }
            } else {
                sender.sendMessage(new TextComponentString("Invalid input: '" + mode + "'. Expected 'feet' or 'below'"));
                throw new CommandException("Usage: add <feet|below> <DEFAULT|NATURAL|ALWAYS_TICK>");
            }

            String type = args[2].toUpperCase();
            UpdateType updateType;
            try{
                updateType = UpdateType.valueOf(type);
            }catch (IllegalArgumentException e){
                throw new CommandException("Invalid update type! Valid types: " + Arrays.toString(UpdateType.values()));
            }

            SCHEDULER.schedule(new Runnable() {
                @Override
                public void run() {
                    ResourceLocation resourceLocation = Block.REGISTRY.getNameForObject(blockToAdd);
                    String identifier = resourceLocation.getResourceDomain() + ":" + resourceLocation.getResourcePath();

                    /* Clear current status */
                    {
                        ArrayList<String> list = new ArrayList<>(Arrays.asList(TiqualityConfig.AUTO_WORLD_ASSIGNED_OBJECTS));
                        list.remove(identifier);
                        TiqualityConfig.AUTO_WORLD_ASSIGNED_OBJECTS = list.toArray(new String[0]);
                    }
                    {
                        ArrayList<String> list = new ArrayList<>(Arrays.asList(TiqualityConfig.TICKFORCING));
                        list.remove(identifier);
                        TiqualityConfig.TICKFORCING = list.toArray(new String[0]);

                    }
                    /* Recalculate inheritance */
                    if(updateType == UpdateType.NATURAL) {
                        ArrayList<String> list = new ArrayList<>(Arrays.asList(TiqualityConfig.AUTO_WORLD_ASSIGNED_OBJECTS));
                        list.add(identifier);
                        TiqualityConfig.AUTO_WORLD_ASSIGNED_OBJECTS = list.toArray(new String[0]);
                    }else if(updateType == UpdateType.ALWAYS_TICK){
                        ArrayList<String> list = new ArrayList<>(Arrays.asList(TiqualityConfig.TICKFORCING));
                        list.add(identifier);
                        TiqualityConfig.TICKFORCING = list.toArray(new String[0]);
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

            player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Claiming area: x=" + leastPos.getX() + " z=" + leastPos.getZ() + " to x=" + mostPos.getX() + " z=" + mostPos.getZ()));
            Tracker tracker = PlayerTracker.getOrCreatePlayerTrackerByProfile((TiqualityWorld) player.world,player.getGameProfile());
            ((TiqualityWorld) player.getEntityWorld()).setTiqualityTrackerCuboidAsync(leastPos, mostPos, tracker, new Runnable() {
                @Override
                public void run() {
                    player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Done."));
                }
            });
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
            ArrayList<Tracker> trackersToProfile = new ArrayList<>();


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
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Profiler started"));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SimpleProfiler profiler = new SimpleProfiler(trackersToProfile.toArray(new Tracker[0]));
                        profiler.start();
                        Thread.sleep(time * 1000);
                        profiler.stop();
                        for(TextComponentString line : profiler.createReport()){
                            sender.sendMessage(line);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            },"Tiquality profile processing thread").start();

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
            if(holder.hasPermission(PermissionHolder.Permission.ADMIN)){
                addIfStartsWith(list, start, "add");
                addIfStartsWith(list, start, "reload");
            }
            return list;
        }else if(args.length == 2){
            String start = args[1];
            if(args[0].equalsIgnoreCase("info")){
                addIfStartsWith(list, start, "point");
            }else if(args[0].equalsIgnoreCase("add")){
                addIfStartsWith(list, start, "below");
                addIfStartsWith(list, start, "feet");
            }else if(args[0].equalsIgnoreCase("track")){
                addIfStartsWith(list, start, "1");
                addIfStartsWith(list, start, "5");
                addIfStartsWith(list, start, "10");
                addIfStartsWith(list, start, "20");
            }else if(args[0].equalsIgnoreCase("profile")){
                addIfStartsWith(list, start, "5");
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
