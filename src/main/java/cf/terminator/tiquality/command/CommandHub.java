package cf.terminator.tiquality.command;

import cf.terminator.tiquality.sponge.TiqualitySpongeHandler;
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
    public static final String PERMISSION_NODE = "tiquality.use";
    public static final String PERMISSION_NODE_ADMIN = "tiquality.admin";
    public static final String DESCRIPTION = "Allows use of Tiquality commands.";
    public static final String DESCRIPTION_ADMIN = "Allows use of Tiquality admin commands.";
    public static final TabCompletionElement TAB_COMPLETION_OPTIONS = TabCompletionElement.generateRoot();

    /**
     * Executed after Sponge had the time to register.
     * We let Sponge give a crack at it first, and take over if there's no sponge detected
     */
    public void initForge(){
        if(registerType == RegisterType.SPONGEFORGE){
            return;
        }else if(registerType != RegisterType.NONE){
            throw new IllegalStateException("Must not register commands twice!");
        }
        registerType = RegisterType.FORGE_ONLY;
        cf.terminator.tiquality.Tiquality.LOGGER.info("Sponge is not detected. Going forge!");

        PermissionAPI.registerNode(PERMISSION_NODE, DefaultPermissionLevel.ALL, DESCRIPTION);
        PermissionAPI.registerNode(PERMISSION_NODE_ADMIN, DefaultPermissionLevel.OP, DESCRIPTION_ADMIN);
        CommandHandler ch = (CommandHandler) FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager();
        ch.registerCommand(TiqualityCommand.INSTANCE);
    }

    /**
     * Executed before the forge command has the time to register.
     * Indicates that the command is registered inside Sponge.
     */
    public void initSponge(){
        if(registerType != RegisterType.NONE){
            throw new IllegalStateException("Must not register commands twice!");
        }
        registerType = RegisterType.SPONGEFORGE;
        cf.terminator.tiquality.Tiquality.LOGGER.info("Sponge is detected. Going Sponge!");
        new TiqualitySpongeHandler().init();
    }
}
