package cf.terminator.tiquality.command;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.monitor.ClaimMonitor;
import cf.terminator.tiquality.monitor.InfoMonitor;
import cf.terminator.tiquality.store.PlayerTracker;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            return "Usage: /tiquality <info [point]|claim|reload|add>";
        }else if (holder.hasPermission(PermissionHolder.Permission.USE)) {
            return "Usage: /tiquality <info [point]|claim>";
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
        }else if(args[0].equalsIgnoreCase("add")){
            holder.checkPermission(PermissionHolder.Permission.ADMIN);
            if(sender instanceof EntityPlayer == false){
                throw new CommandException("Only players can use the 'add' command.");
            }
            EntityPlayer player = (EntityPlayer) sender;
            if(args.length != 2){
                throw new CommandException("Usage: add <feet|below>");
            }
            String mode = args[1];
            Block blockToAdd;
            if(mode.equalsIgnoreCase("feet")){
                blockToAdd = player.getEntityWorld().getBlockState(player.getPosition()).getBlock();
                if(blockToAdd == Blocks.AIR){
                    throw new CommandException("Please stand with your feet in a block (like water) and run this command again.");
                }
            }else if(mode.equalsIgnoreCase("below")){
                blockToAdd = player.getEntityWorld().getBlockState(player.getPosition().down()).getBlock();
                if(blockToAdd == Blocks.AIR){
                    throw new CommandException("Please stand on top of a block and run this command again.");
                }
            }else{
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
