package com.github.terminatornl.tiquality.integration;

import com.github.terminatornl.tiquality.Tiquality;
import com.github.terminatornl.tiquality.integration.ftbutilities.FTBUtilitiesHook;
import com.github.terminatornl.tiquality.integration.griefprevention.GriefPreventionHook;
import me.ryanhamshire.griefprevention.GriefPrevention;
import net.minecraftforge.fml.common.Loader;

import java.util.HashSet;

public class ExternalHooker {

    public static final HashSet<String> LOADED_HOOKS = new HashSet<>();

    public static void init() {
        if (Loader.isModLoaded("griefprevention")) {
            try {
                //noinspection ResultOfMethodCallIgnored
                GriefPrevention.getApi();
                /*
                    GriefPrevention API loaded successfully
                 */
                Tiquality.LOGGER.info("GriefPrevention detected. Adding hooks...");
                LOADED_HOOKS.add("griefprevention");
                GriefPreventionHook.init();
                Tiquality.LOGGER.info("Done.");
            } catch (IllegalStateException e) {
                Tiquality.LOGGER.info("The griefprevention API is not loaded, and therefore, we cannot hook into it.");
            }
        }
        if (Loader.isModLoaded("ftbutilities")) {
            Tiquality.LOGGER.info("FTB Utilities detected. Adding hooks...");
            LOADED_HOOKS.add("ftbutilities");
            FTBUtilitiesHook.init();
            Tiquality.LOGGER.info("Done.");
        }
    }
}
