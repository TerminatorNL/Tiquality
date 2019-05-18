package cf.terminator.tiquality.command;

import cf.terminator.tiquality.Tiquality;
import net.minecraft.command.ICommandSender;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.*;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"WeakerAccess", "NullableProblems"})
public class SpongeCommand implements CommandCallable {

    public void init() {
        CommandManager commandManager = Sponge.getCommandManager();
        for(String alias : CommandHub.ALIASES){
            if(commandManager.containsAlias(alias)){
                Tiquality.LOGGER.info("Command for '" + alias + "' has already been registered, skipping!");
                return;
            }
        }
        Sponge.getCommandManager().register(Tiquality.INSTANCE, this, CommandHub.ALIASES);
    }

    /**
     * Execute the command based on input arguments.
     *
     * <p>The implementing class must perform the necessary permission
     * checks.</p>
     *
     * @param source    The caller of the command
     * @param arguments The raw arguments for this command
     * @return The result of a command being processed
     * @throws CommandException Thrown on a command error
     */
    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        try {
            CommandExecutor.execute((ICommandSender) source, arguments.split(" "), new SpongePermissionHolder(source));
            return CommandResult.success();
        }catch (net.minecraft.command.CommandException e){
            throw new CommandException(Text.of(e.getMessage()), e);
        }
    }

    /**
     * Gets a list of suggestions based on input.
     *
     * <p>If a suggestion is chosen by the user, it will replace the last
     * word.</p>
     *
     * @param source         The command source
     * @param arguments      The arguments entered up to this point
     * @param targetPosition The position the source is looking at when
     *                       performing tab completion
     * @return A list of suggestions
     * @throws CommandException Thrown if there was a parsing error
     */
    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        PermissionHolder holder = new SpongePermissionHolder(source);
        try{
            holder.checkPermission(PermissionHolder.Permission.USE);
            String[] split = arguments.split(" ");
            if(arguments.endsWith(" ")){
                List<String> list = new ArrayList<>(Arrays.asList(split));
                list.add("");
                split = list.toArray(new String[0]);
            }
            return CommandExecutor.getSuggestions(split, holder);
        } catch (net.minecraft.command.CommandException e) {
            throw new CommandException(Text.of(e.getMessage()), e);
        }
    }

    /**
     * Test whether this command can probably be executed by the given source.
     *
     * <p>If implementations are unsure if the command can be executed by
     * the source, {@code true} should be returned. Return values of this method
     * may be used to determine whether this command is listed in command
     * listings.</p>
     *
     * @param source The caller of the command
     * @return Whether permission is (probably) granted
     */
    @Override
    public boolean testPermission(CommandSource source) {
        return new SpongePermissionHolder(source).hasPermission(PermissionHolder.Permission.USE);
    }

    /**
     * Gets a short one-line description of this command.
     *
     * <p>The help system may display the description in the command list.</p>
     *
     * @param source The source of the help request
     * @return A description
     */
    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return Optional.of(Text.of(CommandHub.DESCRIPTION_USE));
    }

    /**
     * Gets a longer formatted help message about this command.
     *
     * <p>It is recommended to use the default text color and style. Sections
     * with text actions (e.g. hyperlinks) should be underlined.</p>
     *
     * <p>Multi-line messages can be created by separating the lines with
     * {@code \n}.</p>
     *
     * <p>The help system may display this message when a source requests
     * detailed information about a command.</p>
     *
     * @param source The source of the help request
     * @return A help text
     */
    @Override
    public Optional<Text> getHelp(CommandSource source) {
        return Optional.of(getUsage(source));
    }

    /**
     * Gets the usage string of this command.
     *
     * <p>A usage string may look like
     * {@code [-w &lt;world&gt;] &lt;var1&gt; &lt;var2&gt;}.</p>
     *
     * <p>The string must not contain the command alias.</p>
     *
     * @param source The source of the help request
     * @return A usage string
     */
    @Override
    public Text getUsage(CommandSource source) {
        return Text.of(CommandExecutor.getUsage(new SpongePermissionHolder(source)));
    }
}
