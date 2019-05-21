package cf.terminator.tiquality.command;

import net.minecraft.command.CommandException;
import net.minecraft.util.text.ITextComponent;

public interface PermissionHolder {
    boolean hasPermission(Permission permission);
    void checkPermission(Permission permission) throws CommandException;
    void sendMessage(ITextComponent message);

    enum Permission{
        USE("tiquality.use"),
        CLAIM("tiquality.claim"),
        ADMIN("tiquality.admin");

        final String node;

        Permission(String node){
            this.node = node;
        }

        public String getNode(){
            return node;
        }
    }
}
