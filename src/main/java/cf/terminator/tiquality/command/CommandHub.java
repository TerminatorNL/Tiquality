package cf.terminator.tiquality.command;

import net.minecraft.command.CommandHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

public class CommandHub {

    public static final CommandHub INSTANCE = new CommandHub();


    public enum RegisterType{
        NONE,
        FORGE_ONLY,
        SPONGEFORGE
    }

    public static RegisterType registerType = RegisterType.NONE;

    public static final String[] ALIASES = {"tiquality","tq"};
    public static final String DESCRIPTION = "Allows use of Tiquality commands.";
    public static final String DESCRIPTION_ADMIN = "Allows use of Tiquality admin commands.";

    /**
     * Sponge is not detected, so we register commands using Forge.
     * This makes sure we do not accidentally load Sponge classes, which aren't present.
     */
    public void initForge(){
        if(registerType != RegisterType.NONE){
            throw new IllegalStateException("Must not register commands twice!");
        }
        registerType = RegisterType.FORGE_ONLY;
        cf.terminator.tiquality.Tiquality.LOGGER.info("Registering command using Forge!");

        PermissionAPI.registerNode(PermissionHolder.Permission.USE.getNode(), DefaultPermissionLevel.ALL, DESCRIPTION);
        PermissionAPI.registerNode(PermissionHolder.Permission.ADMIN.getNode(), DefaultPermissionLevel.OP, DESCRIPTION_ADMIN);
        CommandHandler ch = (CommandHandler) FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager();
        ch.registerCommand(new ForgeCommand());
    }

    /**
     * Registers the command for Sponge
     */
    public void initSponge(){
        if(registerType != RegisterType.NONE){
            throw new IllegalStateException("Must not register commands twice!");
        }
        registerType = RegisterType.SPONGEFORGE;
        cf.terminator.tiquality.Tiquality.LOGGER.info("Registering command for Sponge!");
        new SpongeCommand().init();
    }

    /**
     * Indicate that the server has shut down, allows
     * for re-registering of commands at later start-ups
     */
    public void reset(){
        registerType = RegisterType.NONE;
    }
}
