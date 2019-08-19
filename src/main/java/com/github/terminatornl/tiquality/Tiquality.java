package com.github.terminatornl.tiquality;

import com.github.terminatornl.tiquality.api.Tracking;
import com.github.terminatornl.tiquality.command.CommandHub;
import com.github.terminatornl.tiquality.integration.ExternalHooker;
import com.github.terminatornl.tiquality.interfaces.TickExecutor;
import com.github.terminatornl.tiquality.mixinhelper.MixinValidator;
import com.github.terminatornl.tiquality.monitor.*;
import com.github.terminatornl.tiquality.tracking.ForcedTracker;
import com.github.terminatornl.tiquality.tracking.PlayerTracker;
import com.github.terminatornl.tiquality.tracking.UpdateType;
import com.github.terminatornl.tiquality.tracking.event.EntitySetTrackerEventHandler;
import com.github.terminatornl.tiquality.tracking.tickexecutors.ForgeTickExecutor;
import com.github.terminatornl.tiquality.tracking.tickexecutors.SpongeTickExecutor;
import com.github.terminatornl.tiquality.util.PersistentData;
import com.github.terminatornl.tiquality.util.Scheduler;
import com.github.terminatornl.tiquality.world.WorldHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.Logger;

import java.util.SplittableRandom;

import static com.github.terminatornl.tiquality.mixinhelper.MixinConfigPlugin.MIXIN_CONFIG_PLUGIN_WAS_LOADED;

@SuppressWarnings("WeakerAccess")
@Mod(modid = Tiquality.MODID, name = Tiquality.NAME, version = Tiquality.VERSION, acceptableRemoteVersions = "*")
@IFMLLoadingPlugin.SortingIndex(1001)
public class Tiquality {
    public static final String NAME = "Tiquality";
    public static final String MODID = "tiquality";
    public static final String VERSION = "${version}";
    public static final String DESCRIPTION = "Evenly distribute tick time amongst player, its time we all tick equally!";
    public static final String URL = "https://minecraft.curseforge.com/projects/tiquality";
    public static final String[] AUTHORS = {"Terminator_NL"};
    public static final String PREFIX = TextFormatting.DARK_GRAY + "[" + TextFormatting.GREEN + Tiquality.NAME + TextFormatting.DARK_GRAY + "] " + TextFormatting.GRAY;
    public static final SplittableRandom RANDOM = new SplittableRandom();
    /*
     * Added for readability and convenience
     */
    public static final TPSMonitor TPS_MONITOR = TPSMonitor.INSTANCE;
    public static final Scheduler SCHEDULER = Scheduler.INSTANCE;
    public static TickExecutor TICK_EXECUTOR = new ForgeTickExecutor();
    /**
     * Is also the sponge container.
     */
    public static Tiquality INSTANCE;
    public static Logger LOGGER;
    private TickMaster TICK_MASTER;

    public Tiquality() {
        INSTANCE = this;
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static void log_sync(String str) {
        SCHEDULER.schedule(new Runnable() {
            @Override
            public void run() {
                LOGGER.info(str);
            }
        });
    }

    @EventHandler
    public void preinit(FMLPreInitializationEvent e) {
        LOGGER = e.getModLog();

        if (MIXIN_CONFIG_PLUGIN_WAS_LOADED == false) {
            LOGGER.fatal("The MixinConfigPlugin has not been activated. (com.github.terminatornl.tiquality.mixinhelper.MixinConfigPlugin)");
            LOGGER.fatal("To prevent you from wasting your time, the process has ended.");
            LOGGER.fatal("-> Did you put the Tiquality.jar directly in /mods? Sub-folders are not supported.");
            LOGGER.fatal("-> Is mixin in the classpath, and initialized? It should have printed some messages by now.");

            try {
                Class.forName("org.spongepowered.asm.launch.MixinTweaker", false, getClass().getClassLoader());
                LOGGER.fatal("It looks like you actually have Mixin in the classpath... If you're in a development environment, don't forget to add this argument: ");
                LOGGER.fatal("");
                LOGGER.fatal("--tweakClass org.spongepowered.asm.launch.MixinTweaker");
                LOGGER.fatal("");
                LOGGER.fatal("If you are sure you have done the above correctly, please report this to Terminator_NL.");
            } catch (ClassNotFoundException ignored_2) {
                LOGGER.fatal("It looks like you do not have Mixin installed. Please do one of the following solutions: ");
                LOGGER.fatal("- Use the FAT version of Tiquality (Recommended)");
                LOGGER.fatal("- Install SpongeForge");
                LOGGER.fatal("- Install something ships the Mixin library. (See https://github.com/SpongePowered/Mixin)");
                LOGGER.fatal("- Add Mixin to the classpath yourself (See https://github.com/SpongePowered/Mixin)");
            }
            FMLCommonHandler.instance().exitJava(1, true);
        }
        MinecraftForge.EVENT_BUS.register(SCHEDULER);


        /* Used to monitor TPS while testing. */
        //TPSBroadCaster.start();
    }

    @EventHandler
    public void preinit(FMLInitializationEvent e) {
        if (Loader.isModLoaded("sponge")) {
            TICK_EXECUTOR = new SpongeTickExecutor();
        }
    }

    @SubscribeEvent
    public void onClientConnectedToServerEvent(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        UpdateType.WORLD_IS_REMOTE = e.isLocal() == false;
    }

    @EventHandler
    public void onPost(FMLInitializationEvent e) {
        ExternalHooker.init();
        MixinValidator.validate();
    }

    @EventHandler
    public void onPreServerStart(FMLServerAboutToStartEvent e) {
        if (Loader.isModLoaded("sponge")) {
            CommandHub.INSTANCE.initSponge();
        } else {
            CommandHub.INSTANCE.initForge();
        }
        TICK_MASTER = new TickMaster(e.getServer());
        MinecraftForge.EVENT_BUS.register(TICK_MASTER);
        MinecraftForge.EVENT_BUS.register(TPS_MONITOR);
        MinecraftForge.EVENT_BUS.register(BlockPlaceMonitor.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChunkLoadMonitor.INSTANCE);
        MinecraftForge.EVENT_BUS.register(EntityMonitor.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WorldHelper.SmearedAction.INSTANCE);
        MinecraftForge.EVENT_BUS.register(TiqualityConfig.Listener.INSTANCE);
        MinecraftForge.EVENT_BUS.register(EntitySetTrackerEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ServerWorldLoadMonitor.INSTANCE);
        TiqualityConfig.QuickConfig.update();
        Tracking.registerCustomTracker("PlayerTracker", PlayerTracker.class);
        Tracking.registerCustomTracker("Forced", ForcedTracker.class);
    }

    @EventHandler
    public void onServerStopped(FMLServerStoppedEvent e) {
        CommandHub.INSTANCE.reset();
        PersistentData.deactivate();
        if (TICK_MASTER != null) {
            MinecraftForge.EVENT_BUS.unregister(TICK_MASTER);
        }
        MinecraftForge.EVENT_BUS.unregister(TPS_MONITOR);
        MinecraftForge.EVENT_BUS.unregister(BlockPlaceMonitor.INSTANCE);
        MinecraftForge.EVENT_BUS.unregister(ChunkLoadMonitor.INSTANCE);
        MinecraftForge.EVENT_BUS.unregister(EntityMonitor.INSTANCE);
        MinecraftForge.EVENT_BUS.unregister(WorldHelper.SmearedAction.INSTANCE);
        MinecraftForge.EVENT_BUS.unregister(TiqualityConfig.Listener.INSTANCE);
        MinecraftForge.EVENT_BUS.unregister(EntitySetTrackerEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.unregister(ServerWorldLoadMonitor.INSTANCE);
    }


}
