package cf.terminator.tiquality.command;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.TiqualityConfig;
import cf.terminator.tiquality.interfaces.TiqualityWorldServer;
import cf.terminator.tiquality.store.PlayerTracker;
import net.minecraft.block.Block;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.server.permission.PermissionAPI;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cf.terminator.tiquality.TiqualityConfig.QuickConfig.AUTO_WORLD_ASSIGNED_OBJECTS_FAST;
import static cf.terminator.tiquality.command.CommandHub.PERMISSION_NODE_ADMIN;

@SuppressWarnings({"NullableProblems", "JavaDoc", "NoTranslation"})
public class TiqualityCommand implements ICommand {

    public static final TiqualityCommand INSTANCE = new TiqualityCommand();

    private TiqualityCommand(){};

    /**
     * Gets the name of the command
     */
    @Override
    public String getName() {
        return CommandHub.ALIASES[0];
    }

    /**
     * Gets the usage string for the command.
     *
     * @param sender
     */
    @Override
    public String getUsage(ICommandSender sender) {
        return "";
    }

    /**
     * Get a list of aliases for this command. <b>Never return null!</b>
     */
    @Override
    public List<String> getAliases() {
        return Arrays.asList(CommandHub.ALIASES);
    }

    /**
     * Callback for when the command is executed
     *
     * @param server
     * @param sender
     * @param args
     */
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length == 0){
            sender.sendMessage(new TextComponentString("Running Tiquality version: " + Tiquality.VERSION));
            sender.sendMessage(new TextComponentString(""));
            sender.sendMessage(new TextComponentString("Usage: /tiquality <info|claim|reload|add>"));
            throw new CommandException("Hint: Press TAB for suggestions.");
        }
        if(args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
            if(sender instanceof MinecraftServer == false){
                throw new CommandException("Only the console is allowed to execute this command.");
            }else {
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Reloading..."));
                TiqualityConfig.QuickConfig.reloadFromFile();
                TiqualityConfig.QuickConfig.update();
            }
        }else if(args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("i")) {
            if (sender instanceof EntityPlayer == false) {
                throw new CommandException("Only players can use the 'info' command.");
            }
            EntityPlayer player = (EntityPlayer) sender;
            Block blockAtFeet = player.getEntityWorld().getBlockState(player.getPosition()).getBlock();
            Block blockBelowFeet = player.getEntityWorld().getBlockState(player.getPosition().down()).getBlock();

            boolean feetAllowed = AUTO_WORLD_ASSIGNED_OBJECTS_FAST.contains(blockAtFeet);
            boolean belowAllowed = AUTO_WORLD_ASSIGNED_OBJECTS_FAST.contains(blockBelowFeet);

            player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Info:"));
            if (blockAtFeet == Blocks.AIR && blockBelowFeet == Blocks.AIR) {
                throw new CommandException("Please stand on top of a block and run this command again.");
            }
            if (blockBelowFeet != Blocks.AIR) {
                PlayerTracker tracker = ((TiqualityWorldServer) player.getEntityWorld()).getPlayerTracker(player.getPosition().down());
                String indicator = tracker == null ? TextFormatting.AQUA + "Not claimed" : TextFormatting.GREEN + "Claimed by: " + TextFormatting.AQUA + tracker.getOwner().getName();
                player.sendMessage(new TextComponentString(TextFormatting.WHITE + "Block below: " +
                        TextFormatting.YELLOW + Block.REGISTRY.getNameForObject(blockBelowFeet).toString() + TextFormatting.WHITE + " " + (belowAllowed ? TextFormatting.GREEN + "whitelisted" : TextFormatting.AQUA + "claimed only") + TextFormatting.WHITE + " Status: " + indicator));
            }
            if (blockAtFeet != Blocks.AIR) {
                PlayerTracker tracker = ((TiqualityWorldServer) player.getEntityWorld()).getPlayerTracker(player.getPosition());
                String indicator = tracker == null ? TextFormatting.AQUA + "Not claimed" : TextFormatting.GREEN + "Claimed by: " + TextFormatting.AQUA + tracker.getOwner().getName();
                player.sendMessage(new TextComponentString(TextFormatting.WHITE + "Block at feet: " +
                        TextFormatting.YELLOW + Block.REGISTRY.getNameForObject(blockAtFeet).toString() + TextFormatting.WHITE + " " + (feetAllowed ? TextFormatting.GREEN + "whitelisted" : TextFormatting.AQUA + "claimed only") + TextFormatting.WHITE + " Status: " + indicator));
            }
        }else if(args[0].equalsIgnoreCase("add")){
            if(sender instanceof EntityPlayer == false){
                throw new CommandException("Only players can use the 'add' command.");
            }
            EntityPlayer player = (EntityPlayer) sender;
            if(PermissionAPI.hasPermission(player, PERMISSION_NODE_ADMIN) == false){
                throw new CommandException("Sorry, you don't have permission to execute this command!");
            }
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
            cf.terminator.tiquality.Tiquality.SCHEDULER.schedule(new Runnable() {
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
        }else{
            sender.sendMessage(new TextComponentString("Usage: /tiquality <info|claim|reload|add>"));
            sender.sendMessage(new TextComponentString(""));
            throw new CommandException("Hint: Press TAB for suggestions.");
        }
    }

    /**
     * Check if the given ICommandSender has permission to execute this command
     *
     * @param server
     * @param sender
     */
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        if(sender instanceof EntityPlayer) {
            return PermissionAPI.hasPermission((EntityPlayer) sender, CommandHub.PERMISSION_NODE);
        }else{
            return sender instanceof MinecraftServer;
        }
    }

    /**
     * Get a list of options for when the user presses the TAB key
     *
     * @param server
     * @param sender
     * @param args
     * @param targetPos
     */
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return CommandHub.TAB_COMPLETION_OPTIONS.getTabCompletions(args);
    }

    /**
     * Return whether the specified command parameter index is a username parameter.
     *
     * @param args
     * @param index
     */
    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return getName().compareTo(o.getName());
    }
}
