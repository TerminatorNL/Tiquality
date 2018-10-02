package cf.terminator.tiquality.command;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.api.DataProcessing;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.monitor.ClaimMonitor;
import cf.terminator.tiquality.monitor.InfoMonitor;
import cf.terminator.tiquality.store.PlayerTracker;
import cf.terminator.tiquality.store.TickLogger;
import cf.terminator.tiquality.store.TrackerHub;
import cf.terminator.tiquality.util.Entry3;
import cf.terminator.tiquality.util.SynchronizedAction;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import java.util.*;

import static cf.terminator.tiquality.Tiquality.SCHEDULER;
import static cf.terminator.tiquality.TiqualityConfig.QuickConfig.AUTO_WORLD_ASSIGNED_OBJECTS_FAST;

@SuppressWarnings({"NoTranslation", "WeakerAccess"})
public class CommandExecutor {

    public static void incorrectUsageError(ICommandSender sender, PermissionHolder holder) throws CommandException {
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Running Tiquality version: " + TextFormatting.AQUA + Tiquality.VERSION));
        sender.sendMessage(new TextComponentString(""));
        sender.sendMessage(new TextComponentString(getUsage(holder)));
        throw new CommandException("Hint: Press TAB for suggestions.");
    }

    public static String getUsage(PermissionHolder holder){
        if (holder.hasPermission(PermissionHolder.Permission.ADMIN)) {
            return "Usage: /tiquality <info [point]|claim|profile <secs> [target]|reload|add>";
        }else if (holder.hasPermission(PermissionHolder.Permission.USE)) {
            return "Usage: /tiquality <info [point]|claim|profile <secs>>";
        }else{
            return "";
        }
    }

    public static void execute(ICommandSender sender, String[] args, PermissionHolder holder) throws CommandException{
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
            TiqualityConfig.QuickConfig.update();
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Done!"));
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

            boolean feetAllowed = AUTO_WORLD_ASSIGNED_OBJECTS_FAST.contains(blockAtFeet);
            boolean belowAllowed = AUTO_WORLD_ASSIGNED_OBJECTS_FAST.contains(blockBelowFeet);

            player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Info:"));
            if (blockAtFeet == Blocks.AIR && blockBelowFeet == Blocks.AIR) {
                throw new CommandException("Please stand on top of a block and run this command again.");
            }
            if (blockBelowFeet != Blocks.AIR) {
                PlayerTracker tracker = ((TiqualityWorld) player.getEntityWorld()).getPlayerTracker(player.getPosition().down());
                String indicator = tracker == null ? TextFormatting.AQUA + "Not claimed" : TextFormatting.GREEN + "Claimed by: " + TextFormatting.AQUA + tracker.getOwner().getName();
                player.sendMessage(new TextComponentString(TextFormatting.WHITE + "Block below: " +
                        TextFormatting.YELLOW + Block.REGISTRY.getNameForObject(blockBelowFeet).toString() + TextFormatting.WHITE + " " + (belowAllowed ? TextFormatting.GREEN + "whitelisted" : TextFormatting.AQUA + "claimed only") + TextFormatting.WHITE + " Status: " + indicator));
            }
            if (blockAtFeet != Blocks.AIR) {
                PlayerTracker tracker = ((TiqualityWorld) player.getEntityWorld()).getPlayerTracker(player.getPosition());
                String indicator = tracker == null ? TextFormatting.AQUA + "Not claimed" : TextFormatting.GREEN + "Claimed by: " + TextFormatting.AQUA + tracker.getOwner().getName();
                player.sendMessage(new TextComponentString(TextFormatting.WHITE + "Block at feet: " +
                        TextFormatting.YELLOW + Block.REGISTRY.getNameForObject(blockAtFeet).toString() + TextFormatting.WHITE + " " + (feetAllowed ? TextFormatting.GREEN + "whitelisted" : TextFormatting.AQUA + "claimed only") + TextFormatting.WHITE + " Status: " + indicator));
            }
        /*

                CLAIM

         */
        }else if(args[0].equalsIgnoreCase("claim")){
            if(sender instanceof EntityPlayerMP == false){
                throw new CommandException("Only players can use this command!");
            }
            holder.checkPermission(PermissionHolder.Permission.USE);
            int time = 5;
            if(args.length >= 2){
                time = CommandBase.parseInt(args[1],0,60);
            }

            new ClaimMonitor((EntityPlayerMP) sender).start(time * 1000);
        /*

                ADD

         */
        }else if(args[0].equalsIgnoreCase("add")) {
            holder.checkPermission(PermissionHolder.Permission.ADMIN);
            if (sender instanceof EntityPlayer == false) {
                throw new CommandException("Only players can use the 'add' command.");
            }
            EntityPlayer player = (EntityPlayer) sender;
            if (args.length != 2) {
                throw new CommandException("Usage: add <feet|below>");
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
                throw new CommandException("Usage: add <feet|below>");
            }
            SCHEDULER.schedule(new Runnable() {
                @Override
                public void run() {
                    ResourceLocation resourceLocation = Block.REGISTRY.getNameForObject(blockToAdd);
                    String identifier = resourceLocation.getResourceDomain() + ":" + resourceLocation.getResourcePath();

                    ArrayList<String> list = new ArrayList<>(Arrays.asList(TiqualityConfig.AUTO_WORLD_ASSIGNED_OBJECTS));
                    list.add(identifier);
                    TiqualityConfig.AUTO_WORLD_ASSIGNED_OBJECTS = list.toArray(new String[0]);

                    TiqualityConfig.QuickConfig.saveToFile();
                    TiqualityConfig.QuickConfig.update();

                    sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Added: " + TextFormatting.YELLOW + identifier + TextFormatting.GREEN + " to the config!"));
                }
            });
        /*

            PROFILE

         */
        }else if(args[0].equalsIgnoreCase("profile")){
            holder.checkPermission(PermissionHolder.Permission.USE);
            final PlayerTracker tracker;
            if(args.length == 1) {
                incorrectUsageError(sender, holder);
            }
            int time = CommandBase.parseInt(args[1],1,30);
            if(args.length == 3){
                String target = args[2];
                holder.checkPermission(PermissionHolder.Permission.ADMIN);
                tracker = SynchronizedAction.run(new SynchronizedAction.Action<PlayerTracker>() {
                    @Override
                    public void run(SynchronizedAction.DynamicVar<PlayerTracker> variable) {
                        for(Map.Entry<UUID, PlayerTracker> t : TrackerHub.getEntrySet()){
                            PlayerTracker tracker = t.getValue();
                            GameProfile owner = tracker.getOwner();
                            UUID trackerUUID = owner.getId();
                            if((trackerUUID != null && trackerUUID.toString().equals(target)) || target.equalsIgnoreCase(owner.getName())){
                                variable.set(tracker);
                                return;
                            }
                        }
                    }
                });
                if(tracker == null){
                    throw new CommandException("Sorry, the tracker belonging to: " + target + " isn't found.");
                }
            }else{
                if (sender instanceof EntityPlayer == false) {
                    throw new CommandException("You need to specify a playername or UUID.");
                }
                tracker = TrackerHub.getPlayerTrackerByProfile(((EntityPlayer) sender).getGameProfile());
                if(tracker == null){
                    throw new CommandException("Sorry, you do not have a tracker associated!");
                }
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    tracker.setProfileEnabled(true);
                    try {
                        sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Basic profiler started for " + time + " seconds."));
                        Thread.sleep(time * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    TickLogger logger = tracker.stopProfiler();
                    if(logger == null){
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Looks like something stopped your profiler! Please try again."));
                        return;
                    }
                    ArrayList<Entry3<Block, TickLogger.Location, TickLogger.Metrics>> data = DataProcessing.findBlocks(DataProcessing.getLast(DataProcessing.sortByTime(logger),100));
                    for(Entry3<Block, TickLogger.Location, TickLogger.Metrics> e : data){
                        sender.sendMessage(new TextComponentString(
                                TextFormatting.WHITE + e.getFirst().getLocalizedName() + TextFormatting.BLACK + " - "
                                + e.getSecond() + TextFormatting.BLACK + " - "
                                + TextFormatting.WHITE + ((e.getThird().averageNanosPerCall()) / 1000) + TextFormatting.DARK_GRAY + "µs/c "
                                + TextFormatting.WHITE + ((e.getThird().getNanoseconds()) /logger.getTicks()) / 1000 + TextFormatting.DARK_GRAY + "µs/t"
                                //+ TextFormatting.WHITE + e.getThird().getCalls() + TextFormatting.DARK_GRAY + "c"
                        ));
                    }

                    long consumedMicrosPerTick = (DataProcessing.getTotalNanos(logger)/logger.getTicks())/1000;
                    long grantedMicrosPerTick = (logger.getGrantedNanos()/logger.getTicks())/1000;
                    String percentageUsed = String.valueOf(Math.floor((new Long(consumedMicrosPerTick).doubleValue()/new Long(grantedMicrosPerTick).doubleValue()) * 10000D)/100);
                    sender.sendMessage(new TextComponentString(
                            TextFormatting.DARK_GRAY + "Consumed time: " + TextFormatting.WHITE + consumedMicrosPerTick + TextFormatting.DARK_GRAY + "µs/t" + TextFormatting.BLACK + " - "
                                    + TextFormatting.DARK_GRAY + "Granted time: " + TextFormatting.WHITE + grantedMicrosPerTick + TextFormatting.DARK_GRAY + "µs/t" + TextFormatting.BLACK + " - "
                                    + TextFormatting.DARK_GRAY + "(" + TextFormatting.WHITE + percentageUsed + "%" + TextFormatting.DARK_GRAY + ")"
                    ));
                    sender.sendMessage(new TextComponentString(TextFormatting.DARK_GRAY + "Total ticks: " + TextFormatting.WHITE + logger.getTicks()));




                }
            },"Tiquality profile processing thread for: " + tracker.getOwner().getName()).start();
        /*

            DEBUG

         */
        }else if(args[0].equalsIgnoreCase("debug")){
            holder.checkPermission(PermissionHolder.Permission.ADMIN);
            SCHEDULER.schedule(new Runnable() {
                @Override
                public void run() {
                    Set<Map.Entry<UUID, PlayerTracker>> set = TrackerHub.getEntrySet();
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Loaded PlayerTracker objects (" + set.size() + "):"));
                    for(Map.Entry<UUID, PlayerTracker> e : set){
                        sender.sendMessage(new TextComponentString(TextFormatting.AQUA + e.getKey().toString() + ": "));
                        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + e.getValue().toString()));
                        sender.sendMessage(new TextComponentString(""));
                    }
                }
            });
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
            addIfStartsWith(list, start, "claim");
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
            }else if(args[0].equalsIgnoreCase("claim")){
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
                    for(Map.Entry<UUID, PlayerTracker> e : TrackerHub.getEntrySet()){
                        addIfStartsWith(list, start, e.getValue().getOwner().getName());
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
