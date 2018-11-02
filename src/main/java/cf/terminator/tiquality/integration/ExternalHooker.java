package cf.terminator.tiquality.integration;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.integration.griefprevention.GriefPreventionHook;
import net.minecraftforge.fml.common.Loader;

import java.util.HashSet;

public class ExternalHooker {

    public static final HashSet<String> LOADED_HOOKS = new HashSet<>();

    public static void init(){
        if(Loader.isModLoaded("griefprevention")){
            Tiquality.LOGGER.info("GriefPrevention detected. Adding hooks...");
            LOADED_HOOKS.add("griefprevention");
            GriefPreventionHook.init();
            Tiquality.LOGGER.info("Done.");
        }
    }
}
