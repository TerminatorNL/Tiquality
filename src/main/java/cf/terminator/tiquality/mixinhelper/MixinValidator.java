package cf.terminator.tiquality.mixinhelper;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

import static cf.terminator.tiquality.Tiquality.LOGGER;
import static cf.terminator.tiquality.mixinhelper.MixinConfigPlugin.MIXINS_TO_LOAD;

public class MixinValidator {

    public static final MixinValidator INSTANCE = new MixinValidator();

    private int countdown = 10;

    private MixinValidator(){

    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event){
        if(event.phase != TickEvent.Phase.END){
            return;
        }
        countdown--;
        if(countdown == 0){
            MinecraftForge.EVENT_BUS.unregister(this);
            HashMap<String, String> FAILED_OR_UNLOADED_MIXINS = new HashMap<>(MIXINS_TO_LOAD);
            for(String target : FAILED_OR_UNLOADED_MIXINS.values()){
                try {
                    Class clazz = Class.forName(target);
                    LOGGER.info("Loaded mixin target class: " + clazz);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load class: " + target + ". This is required to apply mixins!");
                    e.printStackTrace();
                }
            }
            if(MIXINS_TO_LOAD.size() > 0){
                LOGGER.fatal("Not all required mixins have been applied!");
                LOGGER.fatal("To prevent you from wasting your time, the process has ended.");
                LOGGER.fatal("");
                LOGGER.fatal("Required mixins that have not been applied:");
                for(Map.Entry<String, String> entry : MIXINS_TO_LOAD.entrySet()){
                    LOGGER.fatal("- " + entry.getKey() + " targeting: " + entry.getValue());
                }
                LOGGER.fatal("");
                LOGGER.fatal("This means that Tiquality will not function properly.");
                LOGGER.fatal("Make sure your versions are correct for Forge as well as SpongeForge.");
                LOGGER.fatal("");
                FMLCommonHandler.instance().exitJava(1, true);
            }
        }
    }
}
