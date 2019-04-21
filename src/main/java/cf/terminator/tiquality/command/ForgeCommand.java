package cf.terminator.tiquality.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class ForgeCommand extends CommandBase {

    /**
     * Gets the name of the command
     */
    @Nonnull
    @Override
    public String getName() {
        return CommandHub.ALIASES[0];
    }

    /**
     * Gets the usage string for the command.
     *
     * @param sender the command sender
     */
    @Nonnull
    @Override
    public String getUsage(@Nonnull ICommandSender sender) {
        return "";
    }

    /**
     * Get a list of aliases for this command. <b>Never return null!</b>
     */
    @Nonnull
    @Override
    public List<String> getAliases() {
        return Arrays.asList(CommandHub.ALIASES);
    }

    /**
     * Callback for when the command is executed
     *
     * @param server the MinecraftServer
     * @param sender the command sender
     * @param args arguments
     */
    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException {
        CommandExecutor.execute(sender, args, new ForgePermissionHolder(sender));
    }

    /**
     * Check if the given ICommandSender has permission to execute this command
     *
     * @param server the MinecraftServer
     * @param sender the command sender
     */
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return new ForgePermissionHolder(sender).hasPermission(PermissionHolder.Permission.USE);
    }

    /**
     * Get a list of options for when the user presses the TAB key
     *
     * @param server the MinecraftServer
     * @param sender the command sender
     * @param args arguments
     * @param targetPos the BlockPos of the block the player is looking at.
     */
    @Nonnull
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return CommandExecutor.getSuggestions(args, new ForgePermissionHolder(sender));
    }

    /**
     * Return whether the specified command parameter index is a username parameter.
     *
     * @param args arguments
     * @param index the index
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
