package cf.terminator.tiquality;

import cf.terminator.tiquality.api.Tracking;
import cf.terminator.tiquality.command.CommandHub;
import cf.terminator.tiquality.integration.ExternalHooker;
import cf.terminator.tiquality.interfaces.TickExecutor;
import cf.terminator.tiquality.mixinhelper.MixinValidator;
import cf.terminator.tiquality.monitor.*;
import cf.terminator.tiquality.tracking.ForcedTracker;
import cf.terminator.tiquality.tracking.PlayerTracker;
import cf.terminator.tiquality.tracking.event.EntitySetTrackerEventHandler;
import cf.terminator.tiquality.tracking.tickexecutors.ForgeTickExecutor;
import cf.terminator.tiquality.tracking.tickexecutors.SpongeTickExecutor;
import cf.terminator.tiquality.util.Scheduler;
import cf.terminator.tiquality.world.WorldHelper;
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
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.Logger;

import static cf.terminator.tiquality.mixinhelper.MixinConfigPlugin.MIXIN_CONFIG_PLUGIN_WAS_LOADED;

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
    public static boolean SPONGE_IS_PRESENT = false;
    public static TickExecutor TICK_EXECUTOR;

    /**
     * Is also the sponge container.
     */
    public static Tiquality INSTANCE;

    public static Logger LOGGER;

    /*
     * Added for readability and convenience
     */
    public static final TPSMonitor TPS_MONITOR = TPSMonitor.INSTANCE;
    public static final Scheduler SCHEDULER = Scheduler.INSTANCE;
    public static TickMaster TICK_MASTER;

    @EventHandler
    public void preinit(FMLPreInitializationEvent e){
        INSTANCE = this;
        LOGGER = e.getModLog();

        if(MIXIN_CONFIG_PLUGIN_WAS_LOADED == false){
            LOGGER.fatal("The MixinConfigPlugin has not been activated. (cf.terminator.tiquality.mixinhelper.MixinConfigPlugin)");
            LOGGER.fatal("To prevent you from wasting your time, the process has ended.");
            LOGGER.fatal("-> Did you put the Tiquality.jar directly in /mods? Sub-folders are not supported.");
            LOGGER.fatal("-> Is mixin in the classpath, and initialized? It should have printed some messages by now.");

            try{
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
        MinecraftForge.EVENT_BUS.register(TPS_MONITOR);
        MinecraftForge.EVENT_BUS.register(SCHEDULER);
        MinecraftForge.EVENT_BUS.register(BlockPlaceMonitor.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChunkLoadMonitor.INSTANCE);
        MinecraftForge.EVENT_BUS.register(EntitySpawnMonitor.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ServerWorldLoadMonitor.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WorldHelper.SmearedAction.INSTANCE);


        /* Used to monitor TPS while testing. */
        //TPSBroadCaster.start();

    }

    @EventHandler
    public void onPost(FMLInitializationEvent e){
        ExternalHooker.init();
        MixinValidator.validate();
    }

    @EventHandler
    public void onPreServerStart(FMLServerAboutToStartEvent e){
        if (Loader.isModLoaded("sponge")) {
            SPONGE_IS_PRESENT = true;
            CommandHub.INSTANCE.initSponge();
            TICK_EXECUTOR = new SpongeTickExecutor();
        } else {
            CommandHub.INSTANCE.initForge();
            TICK_EXECUTOR = new ForgeTickExecutor();
        }
        TICK_MASTER = new TickMaster(e.getServer());
        MinecraftForge.EVENT_BUS.register(TICK_MASTER);
        MinecraftForge.EVENT_BUS.register(EntitySetTrackerEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ServerWorldLoadMonitor.INSTANCE);
        TiqualityConfig.QuickConfig.reloadFromFile();
        Tracking.registerCustomTracker("PlayerTracker", PlayerTracker.class);
        Tracking.registerCustomTracker("Forced", ForcedTracker.class);
    }

    @EventHandler
    public void onStop(FMLServerStoppedEvent e){
        CommandHub.INSTANCE.reset();
        if(TICK_MASTER != null){
            MinecraftForge.EVENT_BUS.unregister(TICK_MASTER);
        }
    }

    public static void log_sync(String str){
        SCHEDULER.schedule(new Runnable() {
            @Override
            public void run() {
                LOGGER.info(str);
            }
        });
    }





}
