package cf.terminator.tiquality.command;

import net.minecraft.command.CommandException;

public interface PermissionHolder {
    boolean hasPermission(Permission permission);
    void checkPermission(Permission permission) throws CommandException;

    enum Permission{
        USE("tiquality.use"),
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
