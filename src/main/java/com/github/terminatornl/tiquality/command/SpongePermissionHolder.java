package com.github.terminatornl.tiquality.command;

import net.minecraft.command.CommandException;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

public class SpongePermissionHolder implements PermissionHolder {

    private final CommandSource source;

    public SpongePermissionHolder(CommandSource source) {
        this.source = source;
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return source.hasPermission(permission.getNode());
    }

    @Override
    public void checkPermission(Permission permission) throws CommandException {
        if (hasPermission(permission) == false) {
            throw new CommandException("Sorry, you don't have permission to use this command. (" + permission.getNode() + ")");
        }
    }

    @Override
    public void sendMessage(ITextComponent message) {
        source.sendMessage(Text.of(message.getFormattedText()));
    }
}
