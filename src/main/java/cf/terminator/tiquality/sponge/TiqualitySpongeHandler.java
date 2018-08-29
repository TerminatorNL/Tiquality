package cf.terminator.tiquality.sponge;

import cf.terminator.tiquality.command.CommandHub;
import cf.terminator.tiquality.command.TiqualityCommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("NullableProblems")
public class TiqualitySpongeHandler extends CommandElement implements CommandCallable, CommandExecutor {

    private final MinecraftServer server;
    
    public TiqualitySpongeHandler() {
        super(null);
        server = FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    public void init() {
        CommandSpec spongecommand = CommandSpec.builder()
                .description(Text.of(CommandHub.DESCRIPTION))
                .permission(CommandHub.PERMISSION_NODE)
                .executor(this)
                .build();
        Sponge.getCommandManager().register(cf.terminator.tiquality.Tiquality.INSTANCE, spongecommand, CommandHub.ALIASES);
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
        TiqualityCommand.INSTANCE.checkPermission(server, (ICommandSender) source);
        try {
            TiqualityCommand.INSTANCE.execute(server, (ICommandSender) source, arguments.split(" "));
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
        return CommandHub.TAB_COMPLETION_OPTIONS.getTabCompletions(arguments.split(" "));
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
        return TiqualityCommand.INSTANCE.checkPermission(server, (ICommandSender) source);
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
        return Optional.of(Text.of(CommandHub.DESCRIPTION));
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
        //TODO: IMPLEMENT
        return Optional.of(Text.of("TODO: ..."));
    }

    /**
     * Attempt to extract a value for this element from the given arguments.
     * This method is expected to have no side-effects for the source, meaning
     * that executing it will not change the state of the {@link CommandSource}
     * in any way.
     *
     * @param source The source to parse for
     * @param args   the arguments
     * @return The extracted value
     * @throws ArgumentParseException if unable to extract a value
     */
    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        throw new ArgumentParseException(Text.of("Not implemented!"),"Lazyness",0);
    }

    /**
     * Fetch completions for command arguments.
     *
     * @param src     The source requesting tab completions
     * @param args    The arguments currently provided
     * @param context The context to store state in
     * @return Any relevant completions
     */
    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return CommandHub.TAB_COMPLETION_OPTIONS.getTabCompletions(String.join(" ", args.getAll()).split(" "));
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
        return Text.of(TiqualityCommand.INSTANCE.getUsage((ICommandSender) source));
    }

    /**
     * Callback for the execution of a command.
     *
     * @param src  The commander who is executing this command
     * @param args The parsed command arguments for this command
     * @return the result of executing this command
     * @throws CommandException If a user-facing error occurs while
     *                          executing this command
     */
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {


        String actualArgs = "SPONGEARGS";



        return this.process(src, actualArgs);
    }
}
