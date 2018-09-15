package cf.terminator.tiquality.command;

import net.minecraft.command.CommandException;
import org.spongepowered.api.command.CommandSource;

@SuppressWarnings("NoTranslation")
public class SpongePermissionHolder implements PermissionHolder {

    private final CommandSource source;

    public SpongePermissionHolder(CommandSource source){
        this.source = source;
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return source.hasPermission(permission.getNode());
    }

    @Override
    public void checkPermission(Permission permission) throws CommandException {
        if(hasPermission(permission) == false){
            throw new CommandException("Sorry, you don't have permission to use this command. (" + permission.getNode() + ")");
        }
    }
}
