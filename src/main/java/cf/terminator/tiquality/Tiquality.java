package cf.terminator.tiquality;

import cf.terminator.tiquality.command.CommandHub;
import cf.terminator.tiquality.monitor.BlockPlaceMonitor;
import cf.terminator.tiquality.monitor.TPSMonitor;
import cf.terminator.tiquality.monitor.TickMaster;
import cf.terminator.tiquality.util.Scheduler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
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

    /**
     * Sponge container.
     */
    public static Tiquality INSTANCE;

    public static Logger LOGGER;


    /*
     * Added for readability and convenience
     *
     * TickMaster is not added, because it is not intended to be touched it after registering it.
     */
    public static final TPSMonitor TPS_MONITOR = TPSMonitor.INSTANCE;
    public static final Scheduler SCHEDULER = Scheduler.INSTANCE;
    public static final BlockPlaceMonitor BLOCK_PLACE_MONITOR = BlockPlaceMonitor.INSTANCE;
    public static final CommandHub COMMAND_HUB = CommandHub.INSTANCE;

    @EventHandler
    public void preinit(FMLPreInitializationEvent e){
        INSTANCE = this;
        LOGGER = e.getModLog();

        if(MIXIN_CONFIG_PLUGIN_WAS_LOADED == false){
            LOGGER.fatal("The MixinConfigPlugin has not been activated. (cf.terminator.tiquality.mixinhelper.MixinConfigPlugin)");
            LOGGER.fatal("To prevent you from wasting your time, the process has ended.");
            LOGGER.fatal("-> Is mixin in the classpath, and initialized? It should have printed some messages by now.");

            try{
                Class.forName("org.spongepowered.asm.launch.MixinTweaker", false, getClass().getClassLoader());
                LOGGER.fatal("It looks like you actually have Mixin in the classpath... Please report this to Terminator_NL.");
            } catch (ClassNotFoundException ignored_2) {
                LOGGER.fatal("It looks like you do not have Mixin installed. Please use the FAT version of Tiquality.");
            }
            FMLCommonHandler.instance().exitJava(1, true);
        }

        MinecraftForge.EVENT_BUS.register(TPS_MONITOR);
        MinecraftForge.EVENT_BUS.register(SCHEDULER);
        MinecraftForge.EVENT_BUS.register(BLOCK_PLACE_MONITOR);

        TiqualityConfig.QuickConfig.reloadFromFile();
        TiqualityConfig.QuickConfig.update();

        /* Used to monitor TPS while testing. */
        //TPSBroadCaster.start();



    }


    @EventHandler
    public void onPreServerStart(FMLServerAboutToStartEvent e){
        if (Loader.isModLoaded("sponge")) {
            COMMAND_HUB.initSponge();
        } else {
            COMMAND_HUB.initForge();
        }
        MinecraftForge.EVENT_BUS.register(new TickMaster(e.getServer()));
    }

    @EventHandler
    public void onStop(FMLServerStoppedEvent e){
        COMMAND_HUB.reset();
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
