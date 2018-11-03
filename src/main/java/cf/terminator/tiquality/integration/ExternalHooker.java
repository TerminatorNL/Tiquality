package cf.terminator.tiquality.integration;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.integration.griefprevention.GriefPreventionHook;
import cf.terminator.tiquality.mixinhelper.MixinConfigPlugin;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraftforge.fml.common.Loader;

import java.util.HashSet;

public class ExternalHooker {

    public static final HashSet<String> LOADED_HOOKS = new HashSet<>();

    public static void init(){
        if(Loader.isModLoaded("griefprevention")){

            /*
                Extra check to make sure functionality is active, and I dont get weird bug reports :)
             */

            if(MixinConfigPlugin.griefPreventionPresent == false){
                throw new ReportedException(
                        new CrashReport("Sorry! You have griefprevention installed, but you dont have it in the mods folder with the name 'griefprevention'. Please make sure the jar is in the mods folder, and contains 'griefprevention' in the name.",
                                new IllegalStateException()
                        )
                );
            }
            Tiquality.LOGGER.info("GriefPrevention detected. Adding hooks...");
            LOADED_HOOKS.add("griefprevention");
            GriefPreventionHook.init();
            Tiquality.LOGGER.info("Done.");
        }
    }
}
