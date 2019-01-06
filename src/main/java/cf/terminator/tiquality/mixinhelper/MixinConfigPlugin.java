package cf.terminator.tiquality.mixinhelper;

import cf.terminator.tiquality.Tiquality;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MixinConfigPlugin implements IMixinConfigPlugin{

    public static boolean spongePresent = false;
    public static boolean hasClientClasses = true;
    public static boolean MIXIN_CONFIG_PLUGIN_WAS_LOADED = false;
    public static final Logger LOGGER = LogManager.getLogger("Tiquality-Boot");

    /*
     * Snippet straight out of SpongeForges' source.
     */
    public static boolean isProductionEnvironment(){
        return System.getProperty("net.minecraftforge.gradle.GradleStart.csvDir") == null;
    }

    private void setup(){
        if(MIXIN_CONFIG_PLUGIN_WAS_LOADED == false){
            File thisJar = new File(Tiquality.class.getProtectionDomain().getCodeSource().getLocation().getFile());
            LOGGER.info("I am located here: " + thisJar);
            MIXIN_CONFIG_PLUGIN_WAS_LOADED = true;
            if(getClass().getClassLoader().getResource("net/minecraft/client/main/Main.class") == null){
                hasClientClasses = false;
                LOGGER.info("Loading server classes");
            }else{
                hasClientClasses = true;
                LOGGER.info("Loading client classes");
            }
            try {
                Class.forName("org.spongepowered.mod.SpongeCoremod", false, getClass().getClassLoader());
                LOGGER.info("Sponge is present!");
                spongePresent = true;
            } catch (ClassNotFoundException ignored) {
                LOGGER.info("Sponge is not present!");
                spongePresent = false;
                try{
                    Class.forName("org.spongepowered.asm.launch.MixinTweaker", false, getClass().getClassLoader());
                } catch (ClassNotFoundException ignored_1) {
                    LOGGER.info("Oh no! It looks like you also do not have Mixin installed. Please use the FAT version of Tiquality.");
                    FMLCommonHandler.instance().exitJava(1, true);
                }
            }

            if(isProductionEnvironment()){
                LOGGER.info("We're running in a production environment.");
            }else{
                LOGGER.info("We're running in a deobfuscated environment.");
            }
        }


    }

    private boolean shouldApplyMixin(String mixin){
        if(spongePresent == true){
            switch (mixin){
                case "cf.terminator.tiquality.mixin.MixinWorldServerForge":
                case "cf.terminator.tiquality.mixin.MixinWorldForge":
                    return false;
            }
        }else{
            switch (mixin){
                case "cf.terminator.tiquality.mixin.MixinSpongeTrackingUtil":
                case "cf.terminator.tiquality.mixin.MixinSpongePhaseTracker":
                    return false;
            }
        }
        if(hasClientClasses == false){
            switch (mixin){
                case "cf.terminator.tiquality.mixin.MixinChunkProviderClient":
                case "cf.terminator.tiquality.mixin.MixinWorldClient":
                    return false;
            }
        }
        if(mixin.equals("cf.terminator.tiquality.mixin.MixinHopperlag")) {
            if (isProductionEnvironment() == false) {
                LOGGER.info("Hoppers now have an update time of 5 milliseconds! This is done on purpose, because you don't run in a production environment!");
                return true;
            }else{
                return false;
            }
        }
        return true;
    }

    @Override
    public void onLoad(String mixinPackage) {
        setup();
    }

    @Override
    public String getRefMapperConfig() {
        return "mixins.tiquality.refmap.json";
    }


    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
        boolean shouldApply = shouldApplyMixin(mixin);
        LOGGER.info((shouldApply ? "Enabling: " : "Skipping: ") + mixin);
        return shouldApply;
    }

    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) {

    }

    @Override
    public List<String> getMixins() {
        List<String> list = new ArrayList<>();
        //list.add("");
        return list;
    }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }
}
