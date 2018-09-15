package cf.terminator.tiquality.command;

import cf.terminator.tiquality.Tiquality;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.permission.PermissionAPI;

public class ForgePermissionHolder implements PermissionHolder {

    private final ICommandSender sender;

    public ForgePermissionHolder(ICommandSender sender){
        this.sender = sender;
    }

    @Override
    public boolean hasPermission(Permission permission) {
        if(sender instanceof MinecraftServer){
            return true;
        }else if(sender instanceof EntityPlayer){
            return PermissionAPI.hasPermission(((EntityPlayer) sender), permission.getNode());
        }else{
            Tiquality.LOGGER.info("Unknown command sender type: " + sender.getClass().getName());
            return false;
        }
    }

    @Override
    public void checkPermission(Permission permission) throws CommandException {
        if(hasPermission(permission) == false){
            throw new CommandException("Sorry, you don't have permission to use this command. (" + permission.getNode() + ")");
        }
    }
}
